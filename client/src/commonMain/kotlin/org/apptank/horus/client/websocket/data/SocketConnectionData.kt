package org.apptank.horus.client.websocket.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocketConnectionData(
    @SerialName("socket_id") val socketId: String,
    @SerialName("activity_timeout") val timeout: Int
)