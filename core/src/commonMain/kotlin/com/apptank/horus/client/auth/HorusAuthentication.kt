package com.apptank.horus.client.auth

object HorusAuthentication {

    internal var userAuthentication: UserAuthentication? = null

    fun setupUserAccessToken(token: String) {
        userAuthentication = UserAuthentication(token)
    }

    fun clearSession() {
        userAuthentication = null
    }

    fun setUserActingAs(userId: String) {
        userAuthentication?.setUserActingAs(userId)
    }

    internal fun isUserActingAs(): Boolean {
        return userAuthentication?.isUserActingAs() ?: false
    }

    internal fun isUserAuthenticated(): Boolean {
        return userAuthentication != null
    }

    internal fun getUserAuthenticatedId(): String? {
        return userAuthentication?.userId
    }

    internal fun getUserAccessToken(): String? {
        return userAuthentication?.accessToken
    }

    internal fun getActingAsUserId(): String? {
        return userAuthentication?.getActingAsUserId()
    }
}