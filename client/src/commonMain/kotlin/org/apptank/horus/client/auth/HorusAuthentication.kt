package org.apptank.horus.client.auth

import org.apptank.horus.client.HorusDataFacade
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.exception.UserNotAuthenticatedException

/**
 * HorusAuthentication is responsible for managing user authentication sessions,
 * including setting up user tokens, handling "acting as" functionality,
 * and checking the authentication state.
 *
 * @author John Ospina
 * @year 2024
 */
object HorusAuthentication {

    // Holds the current user authentication session
    internal var userAuthentication: UserAuthentication? = null

    /**
     * Initializes the user authentication session with the given access token.
     *
     * @param token The access token for user authentication.
     */
    fun setupUserAccessToken(token: String) {
        HorusDataFacade.init()
        userAuthentication = UserAuthentication(token)
        EventBus.emit(EventType.SETUP_CHANGED)
    }

    /**
     * Clears the current user authentication session.
     */
    fun clearSession() {
        userAuthentication = null
        EventBus.emit(EventType.USER_SESSION_CLEARED)
    }

    /**
     * Sets the current user to act as another user identified by the given userId.
     *
     * @param userId The ID of the user to act as.
     */
    fun setUserActingAs(userId: String) {
        userAuthentication?.setUserActingAs(userId)
        EventBus.emit(EventType.SETUP_CHANGED)
    }

    /**
     * Clears the current "acting as" user status.
     */
    fun clearUserActingAs() {
        userAuthentication?.clearUserActingAs()
    }

    /**
     * Checks if the user is currently acting as another user.
     *
     * @return `true` if the user is acting as another user, `false` otherwise.
     */
    internal fun isUserActingAs(): Boolean {
        return userAuthentication?.isUserActingAs() ?: false
    }

    /**
     * Checks if there is an active user authentication session.
     *
     * @return `true` if the user is authenticated, `false` otherwise.
     */
    internal fun isUserAuthenticated(): Boolean {
        return userAuthentication != null
    }

    /**
     * Checks if there is no active user authentication session.
     *
     * @return `true` if the user is not authenticated, `false` otherwise.
     */
    internal fun isNotUserAuthenticated(): Boolean {
        return !isUserAuthenticated()
    }

    /**
     * Returns the ID of the authenticated user, or `null` if no user is authenticated.
     *
     * @return The authenticated user's ID, or `null` if unauthenticated.
     */
    internal fun getUserAuthenticatedId(): String? {
        return userAuthentication?.userId
    }

    /**
     * Returns the access token of the authenticated user, or `null` if no user is authenticated.
     *
     * @return The access token, or `null` if unauthenticated.
     */
    internal fun getUserAccessToken(): String? {
        return userAuthentication?.accessToken
    }

    /**
     * Returns the effective user ID, which is the ID of the user the current user is acting as,
     * or the authenticated user's ID if not acting as any other user.
     *
     * @return The effective user's ID.
     * @throws UserNotAuthenticatedException if no user is authenticated.
     */
    internal fun getEffectiveUserId(): String {
        if (isUserActingAs()) {
            return getActingAsUserId()
        }
        return getUserAuthenticatedId() ?: throw UserNotAuthenticatedException()
    }

    /**
     * Returns the ID of the user the current user is acting as.
     *
     * @return The acting user's ID.
     * @throws IllegalStateException if the user is not acting as any other user.
     */
    internal fun getActingAsUserId(): String {
        return userAuthentication?.getActingAsUserId()
            ?: throw IllegalStateException("User is not acting as any user")
    }
}
