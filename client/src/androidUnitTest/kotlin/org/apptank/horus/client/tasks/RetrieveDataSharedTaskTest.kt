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
import org.apptank.horus.client.control.helper.IDataSharedDatabaseHelper
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the RetrieveDataSharedTask class.
 */
class RetrieveDataSharedTaskTest : TestCase() {

    @Mock
    private val settings = mock(classOf<Settings>())

    @Mock
    private val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    private val databaseHelper = mock(classOf<IDataSharedDatabaseHelper>())

    @Mock
    private val syncService = mock(classOf<ISynchronizationService>())

    private val dependsOnTask = getMockSynchronizeDataTask()

    private lateinit var task: RetrieveDataSharedTask

    @Before
    fun setup() {
        task = RetrieveDataSharedTask(
            settings,
            networkValidator,
            databaseHelper,
            syncService,
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
        coVerify { syncService.getDataShared() }.wasNotInvoked()
        verify { databaseHelper.truncate() }.wasNotInvoked()
        verify { databaseHelper.insert(any()) }.wasNotInvoked()
        verify { settings.putLong(any(), any()) }.wasNotInvoked()
    }

    /**
     * When the last download is within the TTL window, the task should return success without operations.
     */
    @Test
    fun `when last download is within TTL then return success without operations`() = runBlocking {
        // Given
        val currentTimeInSeconds = Clock.System.now().epochSeconds
        val recentTimestamp = currentTimeInSeconds - (60 * 60) // 1 hour ago (less than the 24 hour TTL)

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) }.returns(recentTimestamp)

        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        coVerify { syncService.getDataShared() }.wasNotInvoked()
        verify { databaseHelper.truncate() }.wasNotInvoked()
        verify { databaseHelper.insert(any()) }.wasNotInvoked()
        verify { settings.putLong(any(), any()) }.wasNotInvoked()
    }

    /**
     * When the service call returns a failure, the task should still return success.
     */
    @Test
    fun `when service returns failure then still return success`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) }.returns(null)
        coEvery { syncService.getDataShared() }.returns(DataResult.Failure(Exception("Service error")))

        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        coVerify { syncService.getDataShared() }.wasInvoked()
        verify { databaseHelper.truncate() }.wasNotInvoked()
        verify { databaseHelper.insert(any()) }.wasNotInvoked()
        verify { settings.putLong(any(), any()) }.wasNotInvoked()
    }

    /**
     * When the service returns valid data, the task should process and store it successfully.
     */
    @Test
    fun `when service returns data then insert into database and update timestamp`() = runBlocking {
        // Given
        val entity1 = createEntityResponse("entity1", "id1", mapOf("name" to "Entity 1", "value" to 42))
        val entity2 = createEntityResponse("entity2", "id2", mapOf("name" to "Entity 2", "active" to true))

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) }.returns(null)
        coEvery { syncService.getDataShared() }.returns(DataResult.Success(listOf(entity1, entity2)))

        // When
        val result = task.execute(null)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        coVerify { syncService.getDataShared() }.wasInvoked()
        verify { databaseHelper.truncate() }.wasInvoked()
        verify { databaseHelper.insert(any(), any()) }.wasInvoked()
        verify { settings.putLong(eq(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED), any()) }.wasInvoked()
    }

    /**
     * When the service returns data with missing entity name, it should throw an exception.
     */
    @Test(expected = IllegalStateException::class)
    fun `when service returns data with missing entity name then throw exception`(): Unit = runBlocking {
        // Given
        val entity = SyncDTO.Response.Entity(null, mapOf("id" to "id1", "name" to "Entity"))

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) }.returns(null)
        coEvery { syncService.getDataShared() }.returns(DataResult.Success(listOf(entity)))

        // When/Then
        task.execute(null)
    }

    /**
     * When the service returns data with missing entity ID, it should throw an exception.
     */
    @Test(expected = IllegalStateException::class)
    fun `when service returns data with missing entity ID then throw exception`(): Unit = runBlocking {
        // Given
        val entityWithoutId = createEntityResponse("entity1", null, mapOf("name" to "Entity 1"))

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) }.returns(null)
        coEvery { syncService.getDataShared() }.returns(DataResult.Success(listOf(entityWithoutId)))

        // When/Then
        task.execute(null)
    }

    /**
     * When the service returns data with missing entity data, it should throw an exception.
     */
    @Test(expected = IllegalStateException::class)
    fun `when service returns data with missing entity data then throw exception`(): Unit = runBlocking {
        // Given
        val entity = SyncDTO.Response.Entity("entity1", null)

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { settings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) }.returns(null)
        coEvery { syncService.getDataShared() }.returns(DataResult.Success(listOf(entity)))

        // When/Then
        task.execute(null)
    }

    /**
     * Helper function to create a test entity response.
     */
    private fun createEntityResponse(
        entityName: String?,
        entityId: String?,
        attributes: Map<String, Any>
    ): SyncDTO.Response.Entity {
        val data = mutableMapOf<String, Any>()
        if (entityId != null) {
            data["id"] = entityId
        }
        data.putAll(attributes)

        return SyncDTO.Response.Entity(entityName, data)
    }
} 