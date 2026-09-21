package org.apptank.horus.client.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apptank.horus.client.base.coFold
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.extensions.warn
import org.apptank.horus.client.sync.network.service.IBroadcastService
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.dto.toDomain
import org.apptank.horus.client.websocket.data.SocketConnectionData
import org.apptank.horus.client.websocket.data.SubscribeData
import org.apptank.horus.client.websocket.data.WebSocketEvent
import org.apptank.horus.client.websocket.data.WebSocketPusherEventName
import kotlin.math.min

internal class WebSocketSyncEventsSubscriber(
    private val httpClient: HttpClient,
    private val config: HorusConfig,
    private val broadcastService: IBroadcastService
) : RealtimeSyncEventsSubscriber {

    private val decoderJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    internal val outgoingChannel = Channel<WebSocketEvent>(Channel.BUFFERED)

    override suspend fun subscriber(ownerId: String, onActionReceived: (SyncControl.Action) -> Unit) {

        var currentDelay = 1000L

        config.websocketConfig ?: return

        // ---------------------------------------
        // LIFECYCLE SOCKET
        // ---------------------------------------

        while (currentCoroutineContext().isActive) {

            var sendJob: Job? = null

            try {
                val websocketUrl = "${config.websocketConfig.baseUrl}/horus/${config.websocketConfig.authKey}"
                info("[WebSocketSyncEventsSubscriber] Starting WebSocket connection: ${websocketUrl}")

                httpClient.webSocket(urlString = websocketUrl) {
                    currentDelay = 1000L

                    // -------------------------------------
                    // OUTPUT DATA
                    // -------------------------------------

                    sendJob = launch {
                        for (event in outgoingChannel) {
                            val jsonText = decoderJson.encodeToString(event)
                            info("[WebSocketSyncEventsSubscriber] Sending event: $jsonText")
                            send(Frame.Text(jsonText))
                        }
                    }

                    // -------------------------------------
                    // INCOMING DATA
                    // -------------------------------------

                    for (frame in incoming) {
                        processFrame(frame, ownerId, onActionReceived)
                    }
                }
            } catch (e: Exception) {
                sendJob?.cancel()
                warn("[WebSocketSyncEventsSubscriber] Error while receiving WebSocket events: ${e.message}")
                if (!currentCoroutineContext().isActive) break
            }

            // RECONNECTION BACKOFF
            delay(currentDelay)
            currentDelay = min(currentDelay * 2, MAX_DELAY)
        }
    }

    internal suspend fun processFrame(
        frame: Frame,
        ownerId: String,
        onActionReceived: (SyncControl.Action) -> Unit
    ) {
        if (frame is Frame.Text) {
            val event = decoderJson.decodeFromString<WebSocketEvent>(frame.readText())

            when (event.event) {
                WebSocketPusherEventName.PING -> {
                    outgoingChannel.send(
                        WebSocketEvent(WebSocketPusherEventName.PONG)
                    )
                }

                WebSocketPusherEventName.CONNECTION_ESTABLISHED -> {
                    setupSubscriberChannel(ownerId, event)
                }

                WebSocketPusherEventName.SUBSCRIPTION_SUCCEEDED -> {
                    info("[WebSocketSyncEventsSubscriber] Subscription succeeded")
                }

                WebSocketPusherEventName.SYNC_ACTION -> {
                    info("[WebSocketSyncEventsSubscriber] Received Sync Action: ${event.data}")
                    decoderJson.decodeFromString<SyncDTO.Response.SyncAction>(event.data).toDomain()?.let {
                        onActionReceived(it)
                    }
                }

                else -> {
                    info("[WebSocketSyncEventsSubscriber] Unknown event received: $event")
                }
            }
        }
    }

    internal suspend fun setupSubscriberChannel(userOwnerId: String, webSocketEvent: WebSocketEvent) {
        val socketConnectionData = decoderJson.decodeFromString<SocketConnectionData>(webSocketEvent.data)
        val channelName = "private-horus.sync.$userOwnerId"
        info("[WebSocketSyncEventsSubscriber] Channel Authentication...")

        broadcastService.postAuth(socketConnectionData.socketId, "private-horus.sync.$userOwnerId").coFold(
            onSuccess = {
                info("[WebSocketSyncEventsSubscriber] Channel Authentication successful: ${it.auth}")
                outgoingChannel.send(
                    WebSocketEvent(
                        WebSocketPusherEventName.SUBSCRIBE,
                        decoderJson.encodeToString(SubscribeData(it.auth, channelName))
                    )
                )
            },
            onFailure = {
                logException("[WebSocketSyncEventsSubscriber] Error while setting up subscriber channel", it)
            }
        )
    }

    companion object {
        const val MAX_DELAY = 30000L
    }
}