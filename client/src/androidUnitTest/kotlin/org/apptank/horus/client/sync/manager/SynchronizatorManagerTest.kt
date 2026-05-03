package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.matcher.matches
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlinx.datetime.toInstant
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.sync.network.dto.SyncDTO.Response.HashingValidation
import org.apptank.horus.client.utils.SystemTime
import org.junit.Assert

class SynchronizatorManagerTest : TestCase() {

    val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    val operationDatabaseHelper = mock<IOperationDatabaseHelper>(MockMode.autofill)
    val syncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    val synchronizationService = mock<ISynchronizationService>(MockMode.autofill)

    internal lateinit var synchronizatorManager: SynchronizatorManager

    @Before
    fun setup() {
        synchronizatorManager = SynchronizatorManager(
            networkValidator,
            syncControlDatabaseHelper,
            operationDatabaseHelper,
            synchronizationService,
        )

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
    }

    @Test
    fun `when start with network is not available then do nothing`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() } returns (false)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.IDLE, status)
            }
        }

        // Then
        verify(exactly(0)) { syncControlDatabaseHelper.getPendingActions() }
    }

    @Test
    fun `when exists data pending to push then do nothing`() = runBlocking {
        // Given
        val actions = generateSyncActions(SyncControl.ActionType.INSERT)
        every { networkValidator.isNetworkAvailable() } returns (true)
        every { syncControlDatabaseHelper.getPendingActions() } returns (actions)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.IDLE, status)
            }
        }

        // Then
        verify(exactly(0)) { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }
    }

    @Test
    fun `when exists data to sync but there is not checkpoint then synchronize data`() =
        runBlocking {
            // Given
            val actions = generateSyncActions(SyncControl.ActionType.INSERT)
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.INSERT)
            val checkpointTimestamp = 0L
            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                actions
            )
            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            } returns (
                DataResult.Success(responseActions)
            )

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            verifySuspend(exactly(0)) { synchronizationService.getQueueActions(checkpointTimestamp) }
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data without new data`() =
        runBlocking {
            // Given
            val actions = generateSyncActions(SyncControl.ActionType.INSERT)
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.INSERT)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                actions
            )
            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (
                DataResult.Success(responseActions)
            )
            everySuspend { synchronizationService.getQueueActions(checkpointTimestamp) } returns (
                DataResult.Success(responseActions)
            )
            everySuspend { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                emptyList()
            )
            every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) } returns (true)
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data with only inserts`() =
        runBlocking {

            // Given
            val ownNewActions = generateSyncActions(
                SyncControl.ActionType.INSERT,
                Clock.System.now().epochSeconds - 1000
            )
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.INSERT)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (checkpointTimestamp)
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                ownNewActions)

            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (DataResult.Success(responseActions))

            everySuspend { synchronizationService.getQueueActions(checkpointTimestamp) } returns (
                DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) } returns (true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            val insertInvokeExpected = 1
            verifySuspend(exactly(insertInvokeExpected)) { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) }
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data with only updates`() =
        runBlocking {

            // Given
            val ownNewActions = generateSyncActions(
                SyncControl.ActionType.UPDATE,
                Clock.System.now().epochSeconds - 1000
            )
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.UPDATE)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (checkpointTimestamp)
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                ownNewActions)

            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (DataResult.Success(responseActions))

            every { operationDatabaseHelper.queryRecords(any()) } returns (
                listOf(ownNewActions.first().data.mapValues {
                    it.key to it.value.toString()
                })
            )

            everySuspend { synchronizationService.getQueueActions(checkpointTimestamp) } returns (
                DataResult.Success(responseActions))
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

            every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) } returns (true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            val updateInvokeExpected = 1
            verifySuspend(exactly(updateInvokeExpected)) { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) }
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data with only deletes`() =
        runBlocking {

            // Given
            val ownNewActions = generateSyncActions(
                SyncControl.ActionType.DELETE,
                Clock.System.now().epochSeconds - 1000
            )
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.DELETE)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (checkpointTimestamp)
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                ownNewActions)

            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (DataResult.Success(responseActions))

            everySuspend { synchronizationService.getQueueActions(checkpointTimestamp) } returns (
                DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) } returns (true)

            every { syncControlDatabaseHelper.getEntityLevel(any()) } returns (0)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            val deleteInvokeExpected = 1
            verifySuspend(exactly(deleteInvokeExpected)) { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) }
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }
        }

    @Test
    fun `when exists data to sync and there is a checkpoint initial sync was recently then do not nothing`() =
        runBlocking {

            // Given
            val ownNewActions = generateSyncActions(
                SyncControl.ActionType.DELETE,
                Clock.System.now().epochSeconds - 1000
            )
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.DELETE)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (checkpointTimestamp)
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (SystemTime.getCurrentTimestamp())
            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (DataResult.Success(responseActions))

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                ownNewActions)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            verify(exactly(0)) {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data with multiples actions`() =

        runBlocking {

            // Given
            val insertActions = generateSyncActions(
                SyncControl.ActionType.INSERT,
                Clock.System.now().epochSeconds - 1000
            )
            val updateActions = generateSyncActions(
                SyncControl.ActionType.UPDATE,
                Clock.System.now().epochSeconds - 1000
            )
            val deleteActions = generateSyncActions(
                SyncControl.ActionType.DELETE,
                Clock.System.now().epochSeconds - 1000
            )
            val responseActions =
                generateResponseSyncActions(SyncControl.ActionType.DELETE) + generateResponseSyncActions(
                    SyncControl.ActionType.UPDATE
                ) + generateResponseSyncActions(SyncControl.ActionType.INSERT)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                insertActions + updateActions + deleteActions
            )

            every { operationDatabaseHelper.queryRecords(any()) } returns (
                listOf(updateActions.first().data.mapValues {
                    it.key to it.value.toString()
                })
            )
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) } returns (true)

            every { syncControlDatabaseHelper.getEntityLevel(any()) } returns (0)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            verify(exactly(1)) { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) }
            verify(exactly(1)) {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data is failure`() = runBlocking {

        // Given
        val insertActions = generateSyncActions(
            SyncControl.ActionType.INSERT,
            Clock.System.now().epochSeconds - 1000
        )
        val updateActions = generateSyncActions(
            SyncControl.ActionType.UPDATE,
            Clock.System.now().epochSeconds - 1000
        )
        val deleteActions = generateSyncActions(
            SyncControl.ActionType.DELETE,
            Clock.System.now().epochSeconds - 1000
        )
        val responseActions =
            generateResponseSyncActions(SyncControl.ActionType.DELETE) + generateResponseSyncActions(
                SyncControl.ActionType.UPDATE
            ) + generateResponseSyncActions(SyncControl.ActionType.INSERT)
        val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)
        every { networkValidator.isNetworkAvailable() } returns (true)
        every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
            checkpointTimestamp
        )
        every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
            insertActions + updateActions + deleteActions
        )

        every { operationDatabaseHelper.queryRecords(any()) } returns (
            listOf(updateActions.first().data.mapValues {
                it.key to it.value.toString()
            })
        )

        everySuspend {
            synchronizationService.getQueueActions(
                eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                any()
            )
        } returns (DataResult.Success(responseActions))
        every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())


        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) } returns (false)

        every { syncControlDatabaseHelper.getEntityLevel(any()) } returns (0)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.FAILED, status)
            }
        }

        // Then
        delay(50)
        verify(exactly(1)) {
            syncControlDatabaseHelper.addSyncTypeStatus(
                SyncControl.OperationType.CHECKPOINT,
                SyncControl.Status.FAILED
            )
        }
    }

    @Test
    fun `when sync data delete then sort by entity level`() = runBlocking {

        // Given
        val pendingActions = emptyList<SyncControl.Action>()

        val responseActions = generateResponseSyncActions(SyncControl.ActionType.DELETE, "entity1") + generateResponseSyncActions(
            SyncControl.ActionType.DELETE,
            "entity2"
        )
        val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

        every { networkValidator.isNetworkAvailable() } returns (true)
        every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
            checkpointTimestamp
        )
        every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())

        every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (pendingActions)
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

        everySuspend {
            synchronizationService.getQueueActions(
                eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                any()
            )
        } returns (DataResult.Success(responseActions))

        every {
            operationDatabaseHelper.executeOperations(matches<List<DatabaseOperation>> {
                var indexEntity1 = -1
                var indexEntity2 = -1
                it.forEachIndexed { index, operation ->
                    if (operation is DatabaseOperation.DeleteRecord) {
                        if (operation.table == "entity1") {
                            indexEntity1 = index
                        } else if (operation.table == "entity2") {
                            indexEntity2 = index
                        }
                    }
                }
                indexEntity1 > indexEntity2
            }, any(), any())
        } returns true

        every { syncControlDatabaseHelper.getEntityLevel("entity1") } returns (0)
        every { syncControlDatabaseHelper.getEntityLevel("entity2") } returns (1)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
            }
        }

        // Then
        delay(50)
        verify(exactly(1)) {
            syncControlDatabaseHelper.addSyncTypeStatus(
                SyncControl.OperationType.CHECKPOINT,
                SyncControl.Status.COMPLETED
            )
        }
    }

    @Test
    fun `when not exists data to sync and integrity data is good then do nothing`() = runBlocking {
        // Given
        val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()
        val entityNames = listOf("entity1", "entity2", "entity3")
        val entitiesHashes = mutableListOf<DataMap>()
        val entitiesHashesValidation = mutableListOf<SyncDTO.Response.EntityHash>()

        entityNames.forEach { entityName ->
            val hash = randomHash()

            entitiesHashes.add(mapOf(Horus.Attribute.HASH to randomHash()))
            entitiesHashesValidation.add(
                SyncDTO.Response.EntityHash(
                    entityName,
                    SyncDTO.Response.HashingValidation(hash, hash, true)
                )
            )
        }

        every { networkValidator.isNetworkAvailable() } returns (true)
        every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
            checkpointTimestamp
        )
        every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
            emptyList()
        )
        everySuspend {
            synchronizationService.getQueueActions(
                eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                any()
            )
        } returns (DataResult.Success(emptyList()))

        // ---> Get entities name
        every { syncControlDatabaseHelper.getWritableEntityNames() } returns (entityNames)
        // ---> Get entities hash
        every { operationDatabaseHelper.queryRecords(any()) } returns (entitiesHashes)
        // ---> Validate entities data
        everySuspend { synchronizationService.postValidateEntitiesData(any(), any()) } returns (
            DataResult.Success(entitiesHashesValidation)
        )
        // ---> Get entity hashes
        everySuspend { synchronizationService.getEntityHashes(any(), any()) } returns (
            DataResult.Success(emptyList())
        )

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
            }
        }

        // Then
        delay(50)
        verifySuspend(exactly(0)) { synchronizationService.getDataEntity(any(), any(), any()) }
        verifySuspend(exactly(0)) { synchronizationService.getEntityHashes(any(), any()) }
    }

    @Test
    fun `when not exists data to sync and integrity data is bad then restore corrupted data`() =
        runBlocking {
            // Given
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()
            val entityNames = listOf("entity1")
            val entitiesHashes = mutableListOf<DataMap>()
            val entitiesHashesValidation = mutableListOf<SyncDTO.Response.EntityHash>()
            val entityIdHash = mutableListOf<SyncDTO.Response.EntityIdHash>()
            val entityData = SyncDTO.Response.Entity(
                "entity1",
                mapOf("id" to uuid(), "name" to "name")
            )

            entityNames.forEach { entityName ->
                val hash = randomHash()
                val uuid = uuid()
                entitiesHashes.add(
                    mapOf(
                        Horus.Attribute.ID to uuid,
                        Horus.Attribute.HASH to randomHash()
                    )
                )
                entitiesHashesValidation.add(
                    SyncDTO.Response.EntityHash(
                        entityName,
                        SyncDTO.Response.HashingValidation(hash, hash, false)
                    )
                )
                entityIdHash.add(SyncDTO.Response.EntityIdHash(uuid, randomHash()))
            }

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                emptyList()
            )
            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (DataResult.Success(emptyList()))

            // ---> Get entities name
            every { syncControlDatabaseHelper.getWritableEntityNames() } returns (entityNames)
            // ---> Get entities hash
            every { operationDatabaseHelper.queryRecords(any()) } returns (entitiesHashes)
            // ---> Validate entities data
            everySuspend { synchronizationService.postValidateEntitiesData(any(), any()) } returns (
                DataResult.Success(entitiesHashesValidation)
            )
            // ---> Get entity hashes
            everySuspend { synchronizationService.getEntityHashes(any(), any()) } returns (
                DataResult.Success(entityIdHash)
            )
            // ---> Get entity data to restore
            everySuspend { synchronizationService.getDataEntity(any(), any(), any()) } returns (
                DataResult.Success(listOf(entityData))
            )
            // ---> Restore corrupted data
            // --------> Delete corrupted data
            every { operationDatabaseHelper.deleteRecords(any(), any(), any(), any()) } returns (
                DatabaseOperation.Result(true, 1)
            )
            // --------> Insert new data
            every { operationDatabaseHelper.insertWithTransaction(any(), any()) } returns (true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            verifySuspend(exactly(1)) { synchronizationService.getDataEntity(any(), any(), any()) }
            verifySuspend(exactly(1)) { synchronizationService.getEntityHashes(any(), any()) }
            verifySuspend(exactly(1)) { operationDatabaseHelper.deleteRecords(any(), any(), any(), any()) }
            verifySuspend(exactly(1)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
        }

    @Test
    fun `when not exists data to sync and integrity data by missing data is bad then sync missing data`() =
        runBlocking {
            // Given
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()
            val entityNames = listOf("entity1")
            val entitiesHashes = mutableListOf<DataMap>()
            val entitiesHashesValidation = mutableListOf<SyncDTO.Response.EntityHash>()
            val entityIdHash = mutableListOf<SyncDTO.Response.EntityIdHash>()
            val entityData = SyncDTO.Response.Entity(
                "entity1",
                mapOf("id" to uuid(), "name" to "name")
            )

            val entitiesIdHashesRemote = mutableListOf<SyncDTO.Response.EntityIdHash>()

            entityNames.forEach { entityName ->
                val hash = randomHash()
                val uuid = uuid()
                entitiesHashes.add(
                    mapOf(
                        Horus.Attribute.ID to uuid,
                        Horus.Attribute.HASH to hash
                    )
                )
                entitiesHashesValidation.add(
                    SyncDTO.Response.EntityHash(entityName, HashingValidation(hash, hash, false))
                )
                entityIdHash.add(SyncDTO.Response.EntityIdHash(uuid, hash))
            }

            // Populate remote data
            generateRandomArray {
                entitiesIdHashesRemote.add(SyncDTO.Response.EntityIdHash(uuid(), randomHash()))
            }

            entityIdHash.forEach {
                entitiesIdHashesRemote.add(it)
            }


            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (
                emptyList()
            )
            everySuspend {
                synchronizationService.getQueueActions(
                    checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP,
                    emptyList()
                )
            } returns (DataResult.Success(emptyList()))

            // ---> Get entities name
            every { syncControlDatabaseHelper.getWritableEntityNames() } returns (entityNames)
            // ---> Get entities hash
            every { operationDatabaseHelper.queryRecords(any()) } returns (entitiesHashes)
            // ---> Validate entities data
            everySuspend { synchronizationService.postValidateEntitiesData(any(), any()) } returns (
                DataResult.Success(entitiesHashesValidation)
            )
            // ---> Get entity hashes
            everySuspend { synchronizationService.getEntityHashes(any(), any()) } returns (
                DataResult.Success(entitiesIdHashesRemote)
            )
            // ---> Get entity data to restore
            everySuspend { synchronizationService.getDataEntity(any(), any(), any()) } returns (
                DataResult.Success(listOf(entityData))
            )
            // ---> Sync missing data
            every { operationDatabaseHelper.insertWithTransaction(any(), any()) } returns (true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            delay(50)
            verifySuspend(exactly(1)) { synchronizationService.getDataEntity(any(), any(), any()) }
            verifySuspend(exactly(1)) { synchronizationService.getEntityHashes(any(), any()) }
            verifySuspend(exactly(0)) { operationDatabaseHelper.deleteRecords(any(), any(), any(), any()) }
            verifySuspend(exactly(1)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
        }


    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize with move actions with update success`() = runBlocking {

        // Given
        val responseActions = generateResponseSyncActions(SyncControl.ActionType.MOVE)
        val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

        every { networkValidator.isNetworkAvailable() } returns (true)
        every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (checkpointTimestamp)
        every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())
        every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (listOf())
        every { operationDatabaseHelper.queryRecords(any()) } returns (
            responseActions.map { it.data ?: mapOf() }
        )
        everySuspend {
            synchronizationService.getQueueActions(
                eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                any()
            )
        } returns (DataResult.Success(responseActions))
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Boolean>(), any<Callback>()) } returns (true)
        every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Callback>()) } returns true

        every { syncControlDatabaseHelper.getEntityLevel(any()) } returns (0)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
            }
        }

        // Then
        delay(50)
        verify(exactly(1)) {
            syncControlDatabaseHelper.addSyncTypeStatus(
                SyncControl.OperationType.CHECKPOINT,
                SyncControl.Status.COMPLETED
            )
        }
    }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize with move actions with update failure then delete records`() =
        runBlocking {

            // Given
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.MOVE)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() } returns (true)
            every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (checkpointTimestamp)
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (listOf())
            every { syncControlDatabaseHelper.getEntityNames() } returns (listOf("entity"))
            every { syncControlDatabaseHelper.getEntityLevel("entity") } returns (1)
            every { syncControlDatabaseHelper.getEntitiesRelated("entity") } returns (listOf())
            every { operationDatabaseHelper.deleteRecords(any(), any(), any(), any()) } returns (
                DatabaseOperation.Result(true, 1)
            )
            every { syncControlDatabaseHelper.getExistsActionSequences(any()) } returns (listOf())
            every { operationDatabaseHelper.executeOperations(any<List<DatabaseOperation>>(), any<Callback>()) } returns false
            every { operationDatabaseHelper.queryRecords(any()) } returns (
                responseActions.map { it.data ?: mapOf() }
            )
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

            everySuspend {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                    any()
                )
            } returns (DataResult.Success(responseActions))
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

            every {
                operationDatabaseHelper.executeOperations(matches<List<DatabaseOperation>> {
                    it.all { it is DatabaseOperation.UpdateRecord }
                }, any(), any())
            } returns false

            every {
                operationDatabaseHelper.executeOperations(matches<List<DatabaseOperation>> {
                    it.all { it is DatabaseOperation.DeleteRecord }
                }, any(), any())
            } returns true

            every { syncControlDatabaseHelper.getEntityLevel(any()) } returns (0)
            every { operationDatabaseHelper.countRecords(any()) } returns (1)
            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            verify(exactly(1)) {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }
        }


    @Test
    fun `when exists actionsSequences already process then do nothing`() = runBlocking {
        // Given
        val responseActions = generateResponseSyncActions(SyncControl.ActionType.INSERT)
        val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()
        val existingSequences = responseActions.mapNotNull { it.sequence }

        every { networkValidator.isNetworkAvailable() } returns (true)
        every { syncControlDatabaseHelper.getPendingActions() } returns (emptyList())
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() } returns (checkpointTimestamp)
        every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) } returns (listOf())
        everySuspend {
            synchronizationService.getQueueActions(
                eq(checkpointTimestamp - SynchronizatorManager.CHECKPOINT_GAP),
                any()
            )
        } returns (DataResult.Success(responseActions))

        every { syncControlDatabaseHelper.getExistsActionSequences(existingSequences) } returns (existingSequences)
        every { operationDatabaseHelper.executeOperations(eq(emptyList()), any(), any()) } returns (true)
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns (0L)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
            }
        }

        // Then
        delay(50)
        verifySuspend(exactly(1)) { operationDatabaseHelper.executeOperations(eq(emptyList()), any(), any()) }
        verify(exactly(1)) {
            syncControlDatabaseHelper.addSyncTypeStatus(
                SyncControl.OperationType.CHECKPOINT,
                SyncControl.Status.COMPLETED
            )
        }
    }

    private fun generateSyncActions(
        type: SyncControl.ActionType,
        actionedAt: Long = Clock.System.now().epochSeconds,
        entityName: String = "entity"
    ): List<SyncControl.Action> {
        return generateRandomArray {

            val data = when (type) {
                SyncControl.ActionType.INSERT -> mapOf("id" to uuid(), "name" to "name")
                SyncControl.ActionType.UPDATE, SyncControl.ActionType.MOVE -> mapOf(
                    "id" to uuid(),
                    "attributes" to mapOf("name" to "name")
                )

                SyncControl.ActionType.DELETE -> mapOf("id" to uuid())
            }

            SyncControl.Action(
                Random.nextInt(), type,
                entityName,
                SyncControl.ActionStatus.PENDING,
                data,
                Instant.fromEpochSeconds(actionedAt).toLocalDateTime(TimeZone.UTC)
            )
        }
    }

    private fun generateResponseSyncActions(type: SyncControl.ActionType, entityName: String = "entity"): List<SyncDTO.Response.SyncAction> {
        return generateSyncActions(type).map {
            SyncDTO.Response.SyncAction(
                Random.nextLong(),
                it.action.name,
                entityName,
                it.data,
                it.actionedAt.toInstant(TimeZone.UTC).epochSeconds,
                it.actionedAt.toInstant(TimeZone.UTC).epochSeconds
            )
        }
    }
}