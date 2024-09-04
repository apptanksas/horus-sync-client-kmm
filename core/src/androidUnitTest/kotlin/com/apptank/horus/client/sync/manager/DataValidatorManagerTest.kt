package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.TestCase
import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.database.LocalDatabase
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.eq
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.Dispatchers
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

class DataValidatorManagerTest : TestCase() {

    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())

    @Mock
    val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    lateinit var dataValidatorManager: DataValidatorManager

    @Before
    fun setup() {
        dataValidatorManager = DataValidatorManager(
            networkValidator,
            syncControlDatabaseHelper,
            operationDatabaseHelper,
            synchronizationService,
            Dispatchers.Default,
        )

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
    }

    @Test
    fun `when start with network is not available then do nothing`() {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)

        // When
        dataValidatorManager.start()

        // Then
        verify { syncControlDatabaseHelper.getPendingActions() }.wasNotInvoked()
    }

    @Test
    fun `when exists data pending to push then do nothing`() {
        // Given
        val actions = generateSyncActions(SyncControl.ActionType.INSERT)
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)

        // When
        dataValidatorManager.start()

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
            dataValidatorManager.start()

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
            every { operationDatabaseHelper.executeOperations(listOf(any())) }.returns(true)

            // When
            dataValidatorManager.start()

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

            every { operationDatabaseHelper.executeOperations(listOf(any())) }.returns(true)

            // When
            dataValidatorManager.start()

            // Then
            delay(50)
            val insertInvokeExpected = 1
            coVerify { operationDatabaseHelper.executeOperations(listOf(any())) }.wasInvoked(
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

            every { operationDatabaseHelper.executeOperations(listOf(any())) }.returns(true)

            // When
            dataValidatorManager.start()

            // Then
            delay(50)
            val updateInvokeExpected = 1
            coVerify { operationDatabaseHelper.executeOperations(listOf(any())) }.wasInvoked(
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

            every { operationDatabaseHelper.executeOperations(listOf(any())) }.returns(true)

            // When
            dataValidatorManager.start()

            // Then
            delay(50)
            val deleteInvokeExpected = 1
            coVerify { operationDatabaseHelper.executeOperations(listOf(any()))  }.wasInvoked(
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

            every { operationDatabaseHelper.executeOperations(listOf(any())) }.returns(true)


            // When
            dataValidatorManager.start()

            // Then
            delay(50)
            verify { operationDatabaseHelper.executeOperations(listOf(any())) }.wasInvoked(1)
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

            every { operationDatabaseHelper.executeOperations(listOf(any())) }.returns(false)

            // When
            dataValidatorManager.start()

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
        every { syncControlDatabaseHelper.getEntityNames() }.returns(entityNames)
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
        dataValidatorManager.start()

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
            every { syncControlDatabaseHelper.getEntityNames() }.returns(entityNames)
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
                LocalDatabase.OperationResult(true, 1)
            )
            // --------> Insert new data
            every { operationDatabaseHelper.insertWithTransaction(any(), any()) }.returns(true)

            // When
            dataValidatorManager.start()

            // Then
            delay(50)
            coVerify { synchronizationService.getDataEntity(any(), any(), any()) }.wasInvoked(1)
            coVerify { synchronizationService.getEntityHashes(any()) }.wasInvoked(1)
            coVerify { operationDatabaseHelper.deleteRecords(any(), any(), any()) }.wasInvoked(1)
            coVerify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasInvoked(1)
        }


    private fun generateSyncActions(
        type: SyncControl.ActionType,
        actionedAt: Long = Clock.System.now().epochSeconds
    ): List<SyncControl.Action> {
        return generateArray {

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