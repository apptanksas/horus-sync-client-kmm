package org.apptank.horus.client.auth

import com.russhwolf.settings.Settings
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.mock
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class HorusAuthenticationTest : TestCase() {

    @Mock
    val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())
    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())
    @Mock
    val storageSettings = mock(classOf<Settings>())

    @Before
    fun setup() {
        HorusContainer.setupSyncControlDatabaseHelper(syncControlDatabaseHelper)
        HorusContainer.setupSyncControlDatabaseHelper(syncControlDatabaseHelper)
        HorusContainer.setupSettings(storageSettings)
    }

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

        var eventReceived = false

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)

        EventBus.register(EventType.USER_SESSION_CLEARED){
            eventReceived = true
        }

        HorusAuthentication.clearSession()

        Assert.assertNull(HorusAuthentication.getUserAccessToken())
        Assert.assertFalse(HorusAuthentication.isUserAuthenticated())
        Assert.assertFalse(HorusAuthentication.isUserActingAs())
        Assert.assertTrue(eventReceived)
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
