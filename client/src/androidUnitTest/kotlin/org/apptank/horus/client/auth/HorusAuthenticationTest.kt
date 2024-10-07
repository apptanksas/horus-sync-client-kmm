package org.apptank.horus.client.auth

import org.apptank.horus.client.TestCase
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.junit.Assert
import org.junit.Test


class HorusAuthenticationTest : TestCase() {

    @Test
    fun testSetupUserAccessToken() {

        var eventReceived = false

        EventBus.register(EventType.SETUP_CHANGED){
            eventReceived = true
        }

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        Assert.assertEquals(USER_ACCESS_TOKEN, HorusAuthentication.getUserAccessToken())
        Assert.assertTrue(HorusAuthentication.isUserAuthenticated())
        Assert.assertFalse(HorusAuthentication.isUserActingAs())
        Assert.assertTrue(eventReceived)
    }

    @Test
    fun testClearSession() {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        HorusAuthentication.clearSession()
        Assert.assertNull(HorusAuthentication.getUserAccessToken())
        Assert.assertFalse(HorusAuthentication.isUserAuthenticated())
        Assert.assertFalse(HorusAuthentication.isUserActingAs())
    }

    @Test
    fun testSetUserActingAs() {
        val userActingAs = uuid()
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        HorusAuthentication.setUserActingAs(userActingAs)
        Assert.assertTrue(HorusAuthentication.isUserActingAs())
        Assert.assertEquals(userActingAs, HorusAuthentication.getActingAsUserId())
    }


}