package org.apptank.horus.client.websocket.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WebSocketPusherEventName {
    @SerialName("pusher:ping")
    PING,

    @SerialName("pusher:pong")
    PONG,

    @SerialName("pusher:connection_established")
    CONNECTION_ESTABLISHED,

    @SerialName("pusher_internal:subscription_succeeded")
    SUBSCRIPTION_SUCCEEDED,

    @SerialName("pusher:subscribe")
    SUBSCRIBE,

    @SerialName("horus.sync.action")
    SYNC_ACTION
}