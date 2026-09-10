package org.apptank.horus.client.websocket.data

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketEvent(
    val event: String,
    val data: String = "{}",
    val channel: String? = null
)

@Serializable
data class SubscribeData(
    val auth: String,
    val channel: String
)