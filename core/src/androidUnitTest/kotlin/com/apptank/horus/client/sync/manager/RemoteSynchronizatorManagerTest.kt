package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.TestCase
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import org.junit.Before
import org.junit.Test


class RemoteSynchronizatorManagerTest : TestCase() {


    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    val eventBus = EventBus

    lateinit var remoteSynchronizatorManager: RemoteSynchronizatorManager

    @Before
    fun setup() {
        remoteSynchronizatorManager = RemoteSynchronizatorManager(
            networkValidator,
            syncControlDatabaseHelper,
            synchronizationService,
            eventBus
        )
    }

    @Test
    fun trySynchronizeDataNotExecuteByNetworkNoAvailable() {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)

        // When
        remoteSynchronizatorManager.trySynchronizeData()

        // Then
        verify { syncControlDatabaseHelper.getPendingActions() }.wasNotInvoked()
    }
}