package org.apptank.horus.client.tasks

import com.russhwolf.settings.Settings
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.eq
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the RefreshReadableEntitiesTask class.
 */
class RefreshReadableEntitiesTaskTest : TestCase() {

    @Mock
    private val settings = mock(classOf<Settings>())

    @Mock
    private val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    private val syncService = mock(classOf<ISynchronizationService>())

    @Mock
    private val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())

    @Mock
    private val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

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
            dependsOnTask
        )
    }

    /**
     * When network is not available, the task should return success without further operations.
     */
    @Test
    fun `when network is not available then return success without operations`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)

        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify { syncControlDatabaseHelper.getReadableEntityNames() }.wasNotInvoked()
        coVerify { syncService.getDataEntity(any(), any(), any()) }.wasNotInvoked()
        verify { operationDatabaseHelper.truncate(any()) }.wasNotInvoked()
        verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasNotInvoked()
        verify { settings.putLong(any(), any()) }.wasNotInvoked()
    }

    /**
     * When the last refresh is within the TTL window, the task should return success without operations.
     */
    @Test
    fun `when last refresh is within TTL then return success without operations`() = runBlocking {
        // Given
        val currentTimeInSeconds = Clock.System.now().epochSeconds
        val recentTimestamp = currentTimeInSeconds - (12 * 60 * 60) // 12 hours ago (less than the 24 hour TTL)

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) }.returns(recentTimestamp)

        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify { syncControlDatabaseHelper.getReadableEntityNames() }.wasNotInvoked()
        coVerify { syncService.getDataEntity(any(), any(), any()) }.wasNotInvoked()
        verify { operationDatabaseHelper.truncate(any()) }.wasNotInvoked()
        verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasNotInvoked()
        verify { settings.putLong(any(), any()) }.wasNotInvoked()
    }

    /**
     * When no readable entities are found, the task should return success without fetching data.
     */
    @Test
    fun `when no readable entities found then return success without fetching data`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) }.returns(null)
        every { syncControlDatabaseHelper.getReadableEntityNames() }.returns(emptyList())
        
        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify { syncControlDatabaseHelper.getReadableEntityNames() }.wasInvoked(1)
        coVerify { syncService.getDataEntity(any(), any(), any()) }.wasNotInvoked()
        verify { operationDatabaseHelper.truncate(any()) }.wasNotInvoked()
        verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasNotInvoked()
        verify { settings.putLong(eq(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES), any()) }.wasInvoked(1)
    }

    /**
     * When the service returns valid data for multiple entities, the task should process and store it successfully.
     */
    @Test
    fun `when service returns data for multiple entities then insert all into database`() = runBlocking {
        // Given
        val entityNames = listOf("entity1", "entity2", "entity3")
        
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) }.returns(null)
        every { syncControlDatabaseHelper.getReadableEntityNames() }.returns(entityNames)
        
        // Set up responses for each entity
        coEvery { syncService.getDataEntity("entity1") }.returns(
            DataResult.Success(
                listOf(
                    SyncDTO.Response.Entity("entity1", mapOf("id" to "id1", "name" to "Entity 1 Item 1")),
                    SyncDTO.Response.Entity("entity1", mapOf("id" to "id2", "name" to "Entity 1 Item 2"))
                )
            )
        )
        
        coEvery { syncService.getDataEntity("entity2") }.returns(
            DataResult.Success(
                listOf(
                    SyncDTO.Response.Entity("entity2", mapOf("id" to "id3", "name" to "Entity 2 Item 1"))
                )
            )
        )
        
        coEvery { syncService.getDataEntity("entity3") }.returns(
            DataResult.Success(
                listOf(
                    SyncDTO.Response.Entity("entity3", mapOf("id" to "id4", "name" to "Entity 3 Item 1")),
                    SyncDTO.Response.Entity("entity3", mapOf("id" to "id5", "name" to "Entity 3 Item 2")),
                    SyncDTO.Response.Entity("entity3", mapOf("id" to "id6", "name" to "Entity 3 Item 3"))
                )
            )
        )
        
        every { operationDatabaseHelper.truncate(any()) }.returns(Unit)
        every { operationDatabaseHelper.insertWithTransaction(any(), any()) }.returns(true)
        every { settings.putLong(any(), any()) }

        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify { syncControlDatabaseHelper.getReadableEntityNames() }.wasInvoked(1)
        coVerify { syncService.getDataEntity("entity1") }.wasInvoked(1)
        coVerify { syncService.getDataEntity("entity2") }.wasInvoked(1)
        coVerify { syncService.getDataEntity("entity3") }.wasInvoked(1)
        verify { operationDatabaseHelper.truncate("entity1") }.wasInvoked(1)
        verify { operationDatabaseHelper.truncate("entity2") }.wasInvoked(1)
        verify { operationDatabaseHelper.truncate("entity3") }.wasInvoked(1)
        verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasInvoked(3)
        verify { settings.putLong(eq(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES), any()) }.wasInvoked(1)
    }

    /**
     * When null is passed as the previous task data, the task should still work correctly.
     */
    @Test
    fun `when null previous task data then still execute normally`() = runBlocking {
        // Given
        val entityNames = listOf("entity1")
        
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) }.returns(null)
        every { syncControlDatabaseHelper.getReadableEntityNames() }.returns(entityNames)
        
        coEvery { syncService.getDataEntity("entity1") }.returns(
            DataResult.Success(
                listOf(
                    SyncDTO.Response.Entity("entity1", mapOf("id" to "id1", "name" to "Entity Item 1"))
                )
            )
        )
        
        every { operationDatabaseHelper.truncate(any()) }.returns(Unit)
        every { operationDatabaseHelper.insertWithTransaction(any(), any()) }.returns(true)
        every { settings.putLong(any(), any()) }

        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        verify { syncControlDatabaseHelper.getReadableEntityNames() }.wasInvoked(1)
        coVerify { syncService.getDataEntity("entity1") }.wasInvoked(1)
        verify { operationDatabaseHelper.truncate("entity1") }.wasInvoked(1)
        verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasInvoked(1)
        verify { settings.putLong(eq(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES), any()) }.wasInvoked(1)
    }
} 