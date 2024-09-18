package com.apptank.horus.client.auth

import io.ktor.util.decodeBase64String
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
/**
 * UserAuthentication handles the authentication process of a user based on a JWT access token.
 * It decodes the token to extract user information and allows the user to act as another user.
 *
 * @author John Ospina
 * @year 2024
 */
internal class UserAuthentication(val accessToken: String) {

    // The ID of the authenticated user
    val userId: String

    // The ID of the user the current user is acting as, if any
    private var userActingAs: String? = null

    // JSON decoder configured to ignore unknown keys
    private val decoderJson = Json { ignoreUnknownKeys = true }

    init {
        // Split the JWT token into its parts
        val parts = accessToken.split(".")
        require(parts.size == 3) { "Invalid JWT token format." }

        // Decode the payload part of the JWT
        val payloadJson = parts[1].decodeBase64String()

        // Deserialize the payload into the JWTPayload data class
        val payload = decoderJson.decodeFromString<JWTPayload>(payloadJson)

        // Set the userId based on the token's 'sub' field
        userId = payload.sub
    }

    /**
     * Sets the current user to act as another user identified by the given userId.
     *
     * @param userId The ID of the user to act as.
     */
    fun setUserActingAs(userId: String) {
        userActingAs = userId
    }

    /**
     * Clears the current "acting as" user status.
     */
    fun clearUserActingAs() {
        userActingAs = null
    }

    /**
     * Checks if the user is currently acting as another user.
     *
     * @return `true` if the user is acting as another user, `false` otherwise.
     */
    fun isUserActingAs(): Boolean {
        return userActingAs != null
    }

    /**
     * Returns the ID of the user the current user is acting as, or `null` if not acting as any user.
     *
     * @return The acting user's ID, or `null` if not acting as any user.
     */
    fun getActingAsUserId(): String? {
        return userActingAs
    }
}

/**
 * JWTPayload is a data class used to represent the JWT payload,
 * containing the subject (user ID) of the token.
 *
 * @param sub The subject of the token, which represents the user ID.
 */
@Serializable
internal data class JWTPayload(val sub: String)
