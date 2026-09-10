package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.ILogger
import org.apptank.horus.client.websocket.RealtimeSyncEventsSubscriber
import org.apptank.horus.client.database.builder.QueryBuilder

class RealtimeSynchronizatorManagerTest : TestCase() {

    private val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    private val syncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    private val realtimeSyncEventsSubscriber = mock<RealtimeSyncEventsSubscriber>(MockMode.autofill)
    private val operationDatabaseHelper = mock<IOperationDatabaseHelper>(MockMode.autofill)
    private val logger = mock<ILogger>(MockMode.autofill)
    
    private val testDispatcher = Dispatchers.Unconfined

    private lateinit var realtimeSynchronizatorManager: RealtimeSynchronizatorManager
    private var networkChangeCallback: Callback? = null

    @Before
    fun setup() {
        HorusContainer.setupConfig(getHorusConfigTest())
        HorusContainer.setupLogger(logger)
        
        every { networkValidator.onNetworkChange(any()) } calls { args ->
            networkChangeCallback = args.args[0] as Callback
        }

        realtimeSynchronizatorManager = RealtimeSynchronizatorManager(
            networkValidator,
            syncControlDatabaseHelper,
            realtimeSyncEventsSubscriber,
            operationDatabaseHelper,
            testDispatcher
        )
    }

    @Test
    fun `when start called and network not available then do nothing`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        var subscriberCalledCount = 0
        every { networkValidator.isNetworkAvailable() } returns false
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls {
            subscriberCalledCount++
        }

        realtimeSynchronizatorManager.start()
        delay(200)
        assertEquals("Subscriber should not be called when network unavailable", 0, subscriberCalledCount)
    }

    @Test
    fun `when start called and user not authenticated then do nothing`() = runBlocking {
        try { HorusAuthentication.clearSession() } catch (e: Exception) {}
        var subscriberCalledCount = 0
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls {
            subscriberCalledCount++
        }

        realtimeSynchronizatorManager.start()
        delay(200)
        assertEquals("Subscriber should not be called when user not authenticated", 0, subscriberCalledCount)
    }

    @Test
    fun `when start called and user authenticated and network available then subscribe`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        var subscriberCalledCount = 0
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls {
            subscriberCalledCount++
        }

        realtimeSynchronizatorManager.start()
        delay(200)
        assertEquals("Subscriber should be called once", 1, subscriberCalledCount)
    }

    @Test
    fun `when already active then start does not subscribe again`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        var subscriberCalledCount = 0
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls {
            subscriberCalledCount++
            delay(1000) // Keep the job active
        }

        realtimeSynchronizatorManager.start()
        delay(200)
        realtimeSynchronizatorManager.start()
        delay(200)
        assertEquals("Subscriber should only be called once even if start called twice", 1, subscriberCalledCount)
    }

    @Test
    fun `when network change then start is called`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        var subscriberCalledCount = 0
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls {
            subscriberCalledCount++
        }

        networkChangeCallback?.invoke()
        delay(200)
        assertEquals("Subscriber should be called after network change", 1, subscriberCalledCount)
    }

    @Test
    fun `when subscriber receives action already processed then do nothing`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val eventId = "event-already-processed"
        val action = createSyncAction(eventId = eventId)
        var executeOperationsCalled = 0
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } returns mapOf(eventId to true)

        realtimeSynchronizatorManager.start()
        delay(200)
        assertEquals("executeOperations should not be called for already processed event", 0, executeOperationsCalled)
    }

    @Test
    fun `when subscriber receives new INSERT action then execute operations and add actions completed`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val eventId = "event-insert"
        val action = createSyncAction(eventId = eventId, action = SyncControl.ActionType.INSERT)
        var executeOperationsCalled = 0
        var addActionsCompletedCalled = 0
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } returns mapOf(eventId to false)
        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Callback>()) } calls { args ->
            executeOperationsCalled++
            val callback = args.args[1] as Callback
            callback()
            true
        }
        every { syncControlDatabaseHelper.addActionsCompleted(any<List<SyncControl.Action>>()) } calls {
            addActionsCompletedCalled++
        }

        realtimeSynchronizatorManager.start()
        delay(200)
        
        assertEquals("executeOperations should be called", 1, executeOperationsCalled)
        assertEquals("addActionsCompleted should be called", 1, addActionsCompletedCalled)
    }

    @Test
    fun `when subscriber receives new UPDATE action then execute operations and add actions completed`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val eventId = "event-update"
        val action = createSyncAction(
            eventId = eventId, 
            action = SyncControl.ActionType.UPDATE,
            data = mapOf("id" to "uuid", "attributes" to mapOf("name" to "new"))
        )
        var executeOperationsCalled = 0
        var addActionsCompletedCalled = 0
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } returns mapOf(eventId to false)
        every { operationDatabaseHelper.queryRecords(any<QueryBuilder>()) } returns listOf(mapOf("id" to "uuid", "name" to "old"))
        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Callback>()) } calls { args ->
            executeOperationsCalled++
            val callback = args.args[1] as Callback
            callback()
            true
        }
        every { syncControlDatabaseHelper.addActionsCompleted(any<List<SyncControl.Action>>()) } calls {
            addActionsCompletedCalled++
        }

        realtimeSynchronizatorManager.start()
        delay(300)
        
        assertEquals("executeOperations should be called for UPDATE", 1, executeOperationsCalled)
        assertEquals("addActionsCompleted should be called for UPDATE", 1, addActionsCompletedCalled)
    }

    @Test
    fun `when subscriber receives new DELETE action then execute operations and add actions completed`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val eventId = "event-delete"
        val action = createSyncAction(eventId = eventId, action = SyncControl.ActionType.DELETE)
        var executeOperationsCalled = 0
        var addActionsCompletedCalled = 0
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } returns mapOf(eventId to false)
        every { syncControlDatabaseHelper.getEntityLevel(any<String>()) } returns 0
        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Callback>()) } calls { args ->
            executeOperationsCalled++
            val callback = args.args[1] as Callback
            callback()
            true
        }
        every { syncControlDatabaseHelper.addActionsCompleted(any<List<SyncControl.Action>>()) } calls {
            addActionsCompletedCalled++
        }

        realtimeSynchronizatorManager.start()
        delay(300)
        
        assertEquals("executeOperations should be called for DELETE", 1, executeOperationsCalled)
        assertEquals("addActionsCompleted should be called for DELETE", 1, addActionsCompletedCalled)
    }

    @Test
    fun `when subscriber receives MOVE action then do nothing`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val action = createSyncAction(action = SyncControl.ActionType.MOVE)
        var executeOperationsCalled = 0
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } returns mapOf(action.eventId!! to false)

        realtimeSynchronizatorManager.start()
        delay(300)
        assertEquals("MOVE actions should be ignored", 0, executeOperationsCalled)
    }

    @Test
    fun `when subscriber receives null eventId then do nothing`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val action = createSyncAction(eventId = null)
        var getExistsActionEventIdsCalled = 0
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } calls {
            getExistsActionEventIdsCalled++
            mapOf<String, Boolean>()
        }

        realtimeSynchronizatorManager.start()
        delay(300)
        assertEquals("Action with null eventId should be ignored", 0, getExistsActionEventIdsCalled)
    }

    @Test
    fun `when executeOperations fails then log error`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val eventId = "event-fail"
        val action = createSyncAction(eventId = eventId)
        var errorLogged = false
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } returns mapOf(eventId to false)
        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Callback>()) } returns false
        every { logger.error(any<String>(), any()) } calls {
            errorLogged = true
        }

        realtimeSynchronizatorManager.start()
        delay(300)
        assertEquals("Error should be logged when database operation fails", true, errorLogged)
    }

    @Test
    fun `when processing throws exception then catch and log error`() = runBlocking {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        val eventId = "event-exception"
        val action = createSyncAction(eventId = eventId)
        var errorLogged = false
        
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { realtimeSyncEventsSubscriber.subscriber(any<String>(), any<Function1<SyncControl.Action, Unit>>()) } calls { args ->
            val callback = args.args[1] as (SyncControl.Action) -> Unit
            callback(action)
        }
        every { syncControlDatabaseHelper.getExistsActionEventIds(any<List<String>>()) } returns mapOf(eventId to false)
        // Throw exception inside runCatching block of the manager
        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Callback>()) } throws Exception("Forced error")
        every { logger.error(any<String>(), any()) } calls {
            errorLogged = true
        }

        realtimeSynchronizatorManager.start()
        delay(300)
        assertEquals("Error should be logged when exception occurs", true, errorLogged)
    }

    private fun createSyncAction(
        eventId: String? = "event-id",
        action: SyncControl.ActionType = SyncControl.ActionType.INSERT,
        entity: String = "entity",
        data: Map<String, Any?> = mapOf("id" to "uuid")
    ): SyncControl.Action {
        return SyncControl.Action(
            id = 1,
            action = action,
            entity = entity,
            status = SyncControl.ActionStatus.PENDING,
            data = data,
            actionedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            eventId = eventId
        )
    }
}
