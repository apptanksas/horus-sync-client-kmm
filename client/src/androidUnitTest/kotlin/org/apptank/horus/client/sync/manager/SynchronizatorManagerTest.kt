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
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.eq
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
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
import org.apptank.horus.client.sync.network.dto.SyncDTO.Response.HashingValidation
import org.junit.Assert

class SynchronizatorManagerTest : TestCase() {

    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())

    @Mock
    val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

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
        every { networkValidator.isNetworkAvailable() }.returns(false)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.IDLE, status)
            }
        }

        // Then
        verify { syncControlDatabaseHelper.getPendingActions() }.wasNotInvoked()
    }

    @Test
    fun `when exists data pending to push then do nothing`() = runBlocking {
        // Given
        val actions = generateSyncActions(SyncControl.ActionType.INSERT)
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)

        // When
        synchronizatorManager.start { status, isCompleted ->
            if (isCompleted) {
                Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.IDLE, status)
            }
        }

        // Then
        verify { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.wasNotInvoked()
    }

    @Test
    fun `when exists data to sync but there is not checkpoint then synchronize data`() =
        runBlocking {
            // Given
            val actions = generateSyncActions(SyncControl.ActionType.INSERT)
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.INSERT)
            val checkpointTimestamp = 0L
            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
                actions
            )
            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(
                DataResult.Success(responseActions)
            )

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            coVerify { synchronizationService.getQueueActions(checkpointTimestamp) }.wasNotInvoked()
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data without new data`() =
        runBlocking {
            // Given
            val actions = generateSyncActions(SyncControl.ActionType.INSERT)
            val responseActions = generateResponseSyncActions(SyncControl.ActionType.INSERT)
            val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
                actions
            )
            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(
                DataResult.Success(responseActions)
            )
            coEvery { synchronizationService.getQueueActions(checkpointTimestamp) }.returns(
                DataResult.Success(responseActions)
            )
            coEvery { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
                emptyList()
            )
            every { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.returns(true)

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
            }.wasInvoked()
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

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }
                .returns(checkpointTimestamp)

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }
                .returns(ownNewActions)

            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(DataResult.Success(responseActions))

            coEvery { synchronizationService.getQueueActions(checkpointTimestamp) }
                .returns(DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.returns(true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            val insertInvokeExpected = 1
            coVerify { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.wasInvoked(
                insertInvokeExpected
            )
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }.wasInvoked()
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

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }
                .returns(checkpointTimestamp)

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }
                .returns(ownNewActions)

            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(DataResult.Success(responseActions))

            every { operationDatabaseHelper.queryRecords(any()) }.returns(
                listOf(ownNewActions.first().data.mapValues {
                    it.key to it.value.toString()
                })
            )

            coEvery { synchronizationService.getQueueActions(checkpointTimestamp) }
                .returns(DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.returns(true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            val updateInvokeExpected = 1
            coVerify { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.wasInvoked(
                updateInvokeExpected
            )
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }.wasInvoked()
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

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }
                .returns(checkpointTimestamp)

            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }
                .returns(ownNewActions)

            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(DataResult.Success(responseActions))

            coEvery { synchronizationService.getQueueActions(checkpointTimestamp) }
                .returns(DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.returns(true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            val deleteInvokeExpected = 1
            coVerify { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.wasInvoked(
                deleteInvokeExpected
            )
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }.wasInvoked()
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

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
                insertActions + updateActions + deleteActions
            )

            every { operationDatabaseHelper.queryRecords(any()) }.returns(
                listOf(updateActions.first().data.mapValues {
                    it.key to it.value.toString()
                })
            )

            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.returns(true)


            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            verify { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.wasInvoked(1)
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }.wasInvoked(1)
        }

    @Test
    fun `when exists data to sync and there is a checkpoint then synchronize data is failure`() =

        runBlocking {
            2

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

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
                insertActions + updateActions + deleteActions
            )

            every { operationDatabaseHelper.queryRecords(any()) }.returns(
                listOf(updateActions.first().data.mapValues {
                    it.key to it.value.toString()
                })
            )

            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(DataResult.Success(responseActions))

            every { operationDatabaseHelper.executeOperations(listOf(any()), any()) }.returns(false)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.FAILED, status)
                }
            }

            // Then
            delay(50)
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.FAILED
                )
            }.wasInvoked(1)
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

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(
            checkpointTimestamp
        )
        every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
            emptyList()
        )
        coEvery {
            synchronizationService.getQueueActions(
                eq(checkpointTimestamp),
                any()
            )
        }.returns(DataResult.Success(emptyList()))

        // ---> Get entities name
        every { syncControlDatabaseHelper.getWritableEntityNames() }.returns(entityNames)
        // ---> Get entities hash
        every { operationDatabaseHelper.queryRecords(any()) }.returns(entitiesHashes)
        // ---> Validate entities data
        coEvery { synchronizationService.postValidateEntitiesData(any()) }.returns(
            DataResult.Success(entitiesHashesValidation)
        )
        // ---> Get entity hashes
        coEvery { synchronizationService.getEntityHashes(any()) }.returns(
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
        coVerify { synchronizationService.getDataEntity(any(), any(), any()) }.wasNotInvoked()
        coVerify { synchronizationService.getEntityHashes(any()) }.wasNotInvoked()
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

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
                emptyList()
            )
            coEvery {
                synchronizationService.getQueueActions(
                    eq(checkpointTimestamp),
                    any()
                )
            }.returns(DataResult.Success(emptyList()))

            // ---> Get entities name
            every { syncControlDatabaseHelper.getWritableEntityNames() }.returns(entityNames)
            // ---> Get entities hash
            every { operationDatabaseHelper.queryRecords(any()) }.returns(entitiesHashes)
            // ---> Validate entities data
            coEvery { synchronizationService.postValidateEntitiesData(any()) }.returns(
                DataResult.Success(entitiesHashesValidation)
            )
            // ---> Get entity hashes
            coEvery { synchronizationService.getEntityHashes(any()) }.returns(
                DataResult.Success(entityIdHash)
            )
            // ---> Get entity data to restore
            coEvery { synchronizationService.getDataEntity(any(), any(), any()) }.returns(
                DataResult.Success(listOf(entityData))
            )
            // ---> Restore corrupted data
            // --------> Delete corrupted data
            every { operationDatabaseHelper.deleteRecords(any(), any(), any()) }.returns(
                DatabaseOperation.Result(true, 1)
            )
            // --------> Insert new data
            every { operationDatabaseHelper.insertWithTransaction(any(), any()) }.returns(true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            // Then
            delay(50)
            coVerify { synchronizationService.getDataEntity(any(), any(), any()) }.wasInvoked(1)
            coVerify { synchronizationService.getEntityHashes(any()) }.wasInvoked(1)
            coVerify { operationDatabaseHelper.deleteRecords(any(), any(), any()) }.wasInvoked(1)
            coVerify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasInvoked(1)
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


            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
            every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(
                checkpointTimestamp
            )
            every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
                emptyList()
            )
            coEvery {
                synchronizationService.getQueueActions(
                    checkpointTimestamp,
                    emptyList()
                )
            }.returns(DataResult.Success(emptyList()))

            // ---> Get entities name
            every { syncControlDatabaseHelper.getWritableEntityNames() }.returns(entityNames)
            // ---> Get entities hash
            every { operationDatabaseHelper.queryRecords(any()) }.returns(entitiesHashes)
            // ---> Validate entities data
            coEvery { synchronizationService.postValidateEntitiesData(any()) }.returns(
                DataResult.Success(entitiesHashesValidation)
            )
            // ---> Get entity hashes
            coEvery { synchronizationService.getEntityHashes(any()) }.returns(
                DataResult.Success(entitiesIdHashesRemote)
            )
            // ---> Get entity data to restore
            coEvery { synchronizationService.getDataEntity(any(), any(), any()) }.returns(
                DataResult.Success(listOf(entityData))
            )
            // ---> Sync missing data
            every { operationDatabaseHelper.insertWithTransaction(any(), any()) }.returns(true)

            // When
            synchronizatorManager.start { status, isCompleted ->
                if (isCompleted) {
                    Assert.assertEquals(SynchronizatorManager.SynchronizationStatus.SUCCESS, status)
                }
            }

            delay(50)
            coVerify { synchronizationService.getDataEntity(any(), any(), any()) }.wasInvoked(1)
            coVerify { synchronizationService.getEntityHashes(any()) }.wasInvoked(1)
            coVerify { operationDatabaseHelper.deleteRecords(any(), any(), any()) }.wasNotInvoked()
            coVerify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasInvoked(1)
        }

    private fun generateSyncActions(
        type: SyncControl.ActionType,
        actionedAt: Long = Clock.System.now().epochSeconds
    ): List<SyncControl.Action> {
        return generateRandomArray {

            val data = when (type) {
                SyncControl.ActionType.INSERT -> mapOf("id" to uuid(), "name" to "name")
                SyncControl.ActionType.UPDATE -> mapOf(
                    "id" to uuid(),
                    "attributes" to mapOf("name" to "name")
                )

                SyncControl.ActionType.DELETE -> mapOf("id" to uuid())
            }

            SyncControl.Action(
                Random.nextInt(), type,
                "entity",
                SyncControl.ActionStatus.PENDING,
                data,
                Instant.fromEpochSeconds(actionedAt).toLocalDateTime(TimeZone.UTC)
            )
        }
    }

    private fun generateResponseSyncActions(type: SyncControl.ActionType): List<SyncDTO.Response.SyncAction> {
        return generateSyncActions(type).map {
            SyncDTO.Response.SyncAction(
                it.action.name,
                it.entity,
                it.data,
                it.actionedAt.toInstant(TimeZone.UTC).epochSeconds,
                it.actionedAt.toInstant(TimeZone.UTC).epochSeconds
            )
        }
    }
}