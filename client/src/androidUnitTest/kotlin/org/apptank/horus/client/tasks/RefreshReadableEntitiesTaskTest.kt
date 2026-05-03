package org.apptank.horus.client.tasks

import com.russhwolf.settings.Settings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the RefreshReadableEntitiesTask class.
 */
class RefreshReadableEntitiesTaskTest : TestCase() {

    private val settings = mock<Settings>(MockMode.autofill)
    private val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    private val syncService = mock<ISynchronizationService>(MockMode.autofill)
    private val operationDatabaseHelper = mock<IOperationDatabaseHelper>(MockMode.autofill)
    private val syncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)

    private val dependsOnTask = getMockRetrieveDataSharedTask()

    private lateinit var task: RefreshReadableEntitiesTask

    @Before
    fun setup() {
        task = RefreshReadableEntitiesTask(
            settings,
            networkValidator,
            syncService,
            operationDatabaseHelper,
            syncControlDatabaseHelper,
            24, // TTL of 24 hours
            dependsOnTask
        )
    }

    /**
     * When network is not available, the task should return success without further operations.
     */
    @Test
    fun `when network is not available then return success without operations`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() } returns false

        // When
        val result = task.execute(null, 0, 10)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify(exactly(0)) { syncControlDatabaseHelper.getReadableEntityNames() }
        verifySuspend(exactly(0)) { syncService.getDataEntity(any(), any(), any()) }
        verify(exactly(0)) { operationDatabaseHelper.truncate(any()) }
        verify(exactly(0)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
        verify(exactly(0)) { settings.putLong(any(), any()) }
    }

    /**
     * When the last refresh is within the TTL window, the task should return success without operations.
     */
    @Test
    fun `when last refresh is within TTL then return success without operations`() = runBlocking {
        // Given
        val currentTimeInSeconds = Clock.System.now().epochSeconds
        val recentTimestamp =
            currentTimeInSeconds - (12 * 60 * 60) // 12 hours ago (less than the 24 hour TTL)

        every { networkValidator.isNetworkAvailable() } returns true
        every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) } returns recentTimestamp

        // When
        val result = task.execute(null, 0, 10)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify(exactly(0)) { syncControlDatabaseHelper.getReadableEntityNames() }
        verifySuspend(exactly(0)) { syncService.getDataEntity(any(), any(), any()) }
        verify(exactly(0)) { operationDatabaseHelper.truncate(any()) }
        verify(exactly(0)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
        verify(exactly(0)) { settings.putLong(any(), any()) }
    }

    /**
     * When no readable entities are found, the task should return success without fetching data.
     */
    @Test
    fun `when no readable entities found then return success without fetching data`() =
        runBlocking {
            // Given
            every { networkValidator.isNetworkAvailable() } returns true
            every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) } returns null
            every { syncControlDatabaseHelper.getReadableEntityNames() } returns emptyList()

            // When
            val result = task.execute(null, 0, 10)

            // Then
            Assert.assertTrue(result is TaskResult.Success)
            verify(exactly(1)) { syncControlDatabaseHelper.getReadableEntityNames() }
            verifySuspend(exactly(0)) { syncService.getDataEntity(any(), any(), any()) }
            verify(exactly(0)) { operationDatabaseHelper.truncate(any()) }
            verify(exactly(0)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
            verify(exactly(1)) {
                settings.putLong(
                    eq(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES),
                    any()
                )
            }
        }

    /**
     * When the service returns valid data for multiple entities, the task should process and store it successfully.
     */
    @Test
    fun `when service returns data for multiple entities then insert all into database`() =
        runBlocking {
            // Given
            val entityNames = listOf("entity1", "entity2", "entity3")

            every { networkValidator.isNetworkAvailable() } returns true
            every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) } returns null
            every { syncControlDatabaseHelper.getReadableEntityNames() } returns entityNames

            everySuspend { syncService.getDataEntity("entity1") } returns DataResult.Success(
                listOf(
                    SyncDTO.Response.Entity(
                        "entity1",
                        mapOf("id" to "id1", "name" to "Entity 1 Item 1")
                    ),
                    SyncDTO.Response.Entity(
                        "entity1",
                        mapOf("id" to "id2", "name" to "Entity 1 Item 2")
                    )
                )
            )

            everySuspend { syncService.getDataEntity("entity2") } returns DataResult.Success(
                listOf(
                    SyncDTO.Response.Entity(
                        "entity2",
                        mapOf("id" to "id3", "name" to "Entity 2 Item 1")
                    )
                )
            )

            everySuspend { syncService.getDataEntity("entity3") } returns DataResult.Success(
                listOf(
                    SyncDTO.Response.Entity(
                        "entity3",
                        mapOf("id" to "id4", "name" to "Entity 3 Item 1")
                    ),
                    SyncDTO.Response.Entity(
                        "entity3",
                        mapOf("id" to "id5", "name" to "Entity 3 Item 2")
                    ),
                    SyncDTO.Response.Entity(
                        "entity3",
                        mapOf("id" to "id6", "name" to "Entity 3 Item 3")
                    )
                )
            )

            every { operationDatabaseHelper.truncate(any()) } returns Unit
            every { operationDatabaseHelper.insertWithTransaction(any(), any()) } returns true
            every { settings.putLong(any(), any()) }
            every { syncControlDatabaseHelper.getEntityLevel(any()) } returns 1

            // When
            val result = task.execute(null, 0, 10)

            // Then
            Assert.assertTrue(result is TaskResult.Success)
            verify(exactly(1)) { syncControlDatabaseHelper.getReadableEntityNames() }
            verifySuspend(exactly(1)) { syncService.getDataEntity("entity1") }
            verifySuspend(exactly(1)) { syncService.getDataEntity("entity2") }
            verifySuspend(exactly(1)) { syncService.getDataEntity("entity3") }
            verify(exactly(1)) { operationDatabaseHelper.truncate("entity1") }
            verify(exactly(1)) { operationDatabaseHelper.truncate("entity2") }
            verify(exactly(1)) { operationDatabaseHelper.truncate("entity3") }
            verify(exactly(3)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
            verify(exactly(1)) {
                settings.putLong(
                    eq(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES),
                    any()
                )
            }
        }

    /**
     * When null is passed as the previous task data, the task should still work correctly.
     */
    @Test
    fun `when null previous task data then still execute normally`() = runBlocking {
        // Given
        val entityNames = listOf("entity1")

        every { networkValidator.isNetworkAvailable() } returns true
        every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) } returns null
        every { syncControlDatabaseHelper.getReadableEntityNames() } returns entityNames
        everySuspend { syncService.getDataEntity("entity1") } returns DataResult.Success(
            listOf(
                SyncDTO.Response.Entity(
                    "entity1",
                    mapOf("id" to "id1", "name" to "Entity Item 1")
                )
            )
        )
        every { operationDatabaseHelper.truncate(any()) } returns Unit
        every { operationDatabaseHelper.insertWithTransaction(any(), any()) } returns true
        every { settings.putLong(any(), any()) }

        // When
        val result = task.execute(null, 0, 10)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify(exactly(1)) { syncControlDatabaseHelper.getReadableEntityNames() }
        verifySuspend(exactly(1)) { syncService.getDataEntity("entity1") }
        verify(exactly(1)) { operationDatabaseHelper.truncate("entity1") }
        verify(exactly(1)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
        verify(exactly(1)) {
            settings.putLong(
                eq(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES),
                any()
            )
        }
    }

    @Test
    fun `when the ttl works then execute normally`() = runBlocking {
        // Given
        val entityNames = listOf("entity1")
        val currentTimeInSeconds = Clock.System.now().epochSeconds
        val recentTimestamp =
            currentTimeInSeconds - (25 * 60 * 60) // 25 hours ago (greater than the 24 hour TTL)

        every { networkValidator.isNetworkAvailable() } returns true
        every { syncControlDatabaseHelper.getReadableEntityNames() } returns entityNames
        every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) } returns recentTimestamp
        everySuspend { syncService.getDataEntity("entity1") } returns DataResult.Success(
            listOf(
                SyncDTO.Response.Entity(
                    "entity1",
                    mapOf("id" to "id1", "name" to "Entity Item 1")
                )
            )
        )
        every { operationDatabaseHelper.truncate(any()) } returns Unit
        every { operationDatabaseHelper.insertWithTransaction(any(), any()) } returns true
        every { settings.putLong(any(), any()) }

        // When
        val result = task.execute(null, 0, 10)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify(exactly(1)) { syncControlDatabaseHelper.getReadableEntityNames() }
        verifySuspend(exactly(1)) { syncService.getDataEntity("entity1") }
        verify(exactly(1)) { operationDatabaseHelper.truncate("entity1") }
        verify(exactly(1)) { operationDatabaseHelper.insertWithTransaction(any(), any()) }
        verify(exactly(1)) {
            settings.putLong(
                eq(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES),
                any()
            )
        }
    }
}
