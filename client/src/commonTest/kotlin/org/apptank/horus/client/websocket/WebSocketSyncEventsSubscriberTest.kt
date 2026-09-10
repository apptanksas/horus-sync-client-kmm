package org.apptank.horus.client.websocket

import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.config.WebsocketConfig
import org.apptank.horus.client.config.UploadFilesConfig
import org.apptank.horus.client.sync.network.service.IBroadcastService
import org.apptank.horus.client.websocket.data.WebSocketEvent
import org.apptank.horus.client.websocket.data.WebSocketPusherEventName
import org.apptank.horus.client.sync.network.dto.BroadcastDTO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apptank.horus.client.control.SyncControl

class WebSocketSyncEventsSubscriberTest {

    private val httpClient = HttpClient(MockEngine {
        respond("")
    })
    private val broadcastService = mock<IBroadcastService>()
    private val config = HorusConfig(
        baseUrl = "http://localhost",
        uploadFilesConfig = UploadFilesConfig("", emptyList(), 0),
        websocketConfig = WebsocketConfig("ws://localhost", "authKey")
    )
    private val subscriber = WebSocketSyncEventsSubscriber(httpClient, config, broadcastService)

    @Test
    fun testReceivePingSendsPong() = runBlocking {
        val payload = "{\"event\":\"pusher:ping\"}"
        val frame = Frame.Text(payload)

        subscriber.processFrame(frame, "ownerId") { }

        val response = subscriber.outgoingChannel.receive()
        assertEquals(WebSocketPusherEventName.PONG.id, response.event)
    }

    @Test
    fun testReceiveConnectionEstablishedCallsSetupChannel() = runBlocking {
        val socketId = "397037047.578378505"
        val payload = "{\"event\":\"pusher:connection_established\",\"data\":\"{\\\"socket_id\\\":\\\"$socketId\\\",\\\"activity_timeout\\\":30}\"}"
        val frame = Frame.Text(payload)
        val ownerId = "72df0bb6-e1e8-43d5-95dc-af9b450ddf96"

        everySuspend { broadcastService.postAuth(socketId, "private-horus.sync.$ownerId") } returns DataResult.Success(
            BroadcastDTO.Response.Auth("auth-token")
        )

        subscriber.processFrame(frame, ownerId) { }

        verifySuspend { broadcastService.postAuth(socketId, "private-horus.sync.$ownerId") }
        val response = subscriber.outgoingChannel.receive()
        assertEquals(WebSocketPusherEventName.SUBSCRIBE.id, response.event)
        assertTrue(response.data.contains("auth-token"))
        assertTrue(response.data.contains("private-horus.sync.$ownerId"))
    }

    @Test
    fun testReceiveSubscriptionSucceeded() = runBlocking {
        val payload = "{\"event\":\"pusher_internal:subscription_succeeded\",\"data\":\"{}\",\"channel\":\"private-horus.sync.ownerId\"}"
        val frame = Frame.Text(payload)

        // Does not throw an exception and logs (hard to verify the log without injecting a logger)
        subscriber.processFrame(frame, "ownerId") { }
    }

    @Test
    fun testReceiveSyncActionCallsCallback() = runBlocking {
        val data = "{\\\"event_id\\\":\\\"a0d15115-cb3e-4798-8e25-73e375c23786\\\",\\\"sequence\\\":null,\\\"action\\\":\\\"UPDATE\\\",\\\"entity\\\":\\\"animals\\\",\\\"data\\\":{\\\"id\\\":\\\"12dc40b6-4ee4-4752-a41e-ed54ec5a90e7\\\",\\\"attributes\\\":{\\\"name\\\":\\\"Animal 1\\\"}},\\\"actioned_at\\\":1788983935,\\\"synced_at\\\":1788989368}"
        val payload = "{\"event\":\"horus.sync.action\",\"data\":\"$data\",\"channel\":\"private-horus.sync.ownerId\"}"
        val frame = Frame.Text(payload)

        var actionReceived: SyncControl.Action? = null
        subscriber.processFrame(frame, "ownerId") { action ->
            actionReceived = action
        }

        assertNotNull(actionReceived)
        assertEquals(0, actionReceived?.id)
        assertEquals("a0d15115-cb3e-4798-8e25-73e375c23786", actionReceived?.eventId)
        assertEquals(SyncControl.ActionType.UPDATE, actionReceived?.action)
        assertEquals("animals", actionReceived?.entity)
    }

    @Test
    fun testSetupSubscriberChannelFailure() = runBlocking {
        val socketId = "socket123"
        val data = "{\"event\":\"pusher:connection_established\",\"data\":\"{\\\"socket_id\\\":\\\"$socketId\\\",\\\"activity_timeout\\\":30}\"}"
        val ownerId = "ownerId"

        everySuspend { broadcastService.postAuth(socketId, "private-horus.sync.$ownerId") } returns DataResult.Failure(
            Exception("Auth failed")
        )

        subscriber.setupSubscriberChannel(ownerId, data)

        // It should log the error and not send anything to the channel
        assertTrue(subscriber.outgoingChannel.isEmpty)
    }
}
