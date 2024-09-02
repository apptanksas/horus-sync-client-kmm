package com.apptank.horus.client.auth

import io.ktor.util.decodeBase64String
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class UserAuthentication(val accessToken: String) {

    val userId: String
    private var userActingAs: String? = null

    private val decoderJson = Json { ignoreUnknownKeys = true }

    init {
        val parts = accessToken.split(".")
        require(parts.size == 3) { "Invalid JWT token format." }

        val payloadJson = parts[1].decodeBase64String()

        val payload = decoderJson.decodeFromString<JWTPayload>(payloadJson)

        userId = payload.sub
    }

    fun setUserActingAs(userId: String) {
        userActingAs = userId
    }

    fun clearUserActingAs() {
        userActingAs = null
    }

    fun isUserActingAs(): Boolean {
        return userActingAs != null
    }

    fun getActingAsUserId(): String? {
        return userActingAs
    }
}

@Serializable
internal data class JWTPayload(val sub: String)
