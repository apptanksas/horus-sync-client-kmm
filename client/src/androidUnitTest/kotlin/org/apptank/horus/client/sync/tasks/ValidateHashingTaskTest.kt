package org.apptank.horus.client.sync.tasks

import org.apptank.horus.client.TestCase
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.apptank.horus.client.tasks.TaskResult
import org.apptank.horus.client.tasks.ValidateHashingTask
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ValidateHashingTaskTest : TestCase() {

    val controlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    val synchronizationService = mock<ISynchronizationService>(MockMode.autofill)

    private lateinit var task: ValidateHashingTask

    @Before
    fun setup() {
        task = ValidateHashingTask(
            controlDatabaseHelper,
            synchronizationService,
            getMockValidateMigrationTask()
        )
    }

    @Test
    fun `when status hashing validated is completed then return success`() = runBlocking {
        // Given
        every { controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION) } returns true

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Success)
        verifySuspend(exactly(0)) { synchronizationService.postValidateHashing(any()) }
    }

    @Test
    fun `when status hashing validate is not complete then validate hashing failure`() = runBlocking {
        // Given
        every { controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION) } returns false
        everySuspend { synchronizationService.postValidateHashing(any()) } returns DataResult.Failure(Exception("Hashing validation failed"))

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Failure)
    }

    @Test
    fun `when status hashing validate is not complete then validate hashing success and matched is false`() = runBlocking {
        // Given
        every { controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION) } returns false
        everySuspend { synchronizationService.postValidateHashing(any()) } returns DataResult.Success(
            SyncDTO.Response.HashingValidation(randomHash(), randomHash(), false)
        )

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Failure)
    }

    @Test
    fun `when status hashing validate is not complete then validate hashing success and matched is true`(): Unit = runBlocking {
        // Given
        every { controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION) } returns false
        everySuspend { synchronizationService.postValidateHashing(any()) } returns DataResult.Success(
            SyncDTO.Response.HashingValidation(randomHash(), randomHash(), true)
        )

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Success)
        verify {
            controlDatabaseHelper.addSyncTypeStatus(
                SyncControl.OperationType.HASH_VALIDATION,
                SyncControl.Status.COMPLETED
            )
        }
    }
}