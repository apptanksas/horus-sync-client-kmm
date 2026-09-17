package org.apptank.horus.client.sync.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects (DTOs) for broadcasting-related requests and responses.
 */
sealed class BroadcastDTO {

    /**
     * Sealed class representing various types of broadcasting requests.
     */
    sealed class Request {

        /**
         * Request to authenticate a broadcasting channel.
         *
         * @param socketId The unique socket ID of the connection.
         * @param channelName The name of the channel to authenticate.
         */
        @Serializable
        data class AuthRequest(
            @SerialName("socket_id")
            val socketId: String,
            @SerialName("channel_name")
            val channelName: String
        )
    }

    /**
     * Sealed class representing various types of broadcasting responses.
     */
    sealed class Response {

        /**
         * Response containing the authentication token for a broadcasting channel.
         *
         * @param auth The authentication token.
         */
        @Serializable
        data class Auth(
            @SerialName("auth")
            val auth: String
        )
    }
}
