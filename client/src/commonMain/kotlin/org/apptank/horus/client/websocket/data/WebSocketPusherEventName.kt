package org.apptank.horus.client.websocket.data

enum class WebSocketPusherEventName(val id: String) {
    PING("pusher:ping"),
    PONG("pusher:pong"),
    CONNECTION_ESTABLISHED("pusher:connection_established"),
    SUBSCRIPTION_SUCCEEDED("pusher_internal:subscription_succeeded"),
    SUBSCRIBE("pusher:subscribe"),
    SYNC_ACTION("horus.sync.action")
}