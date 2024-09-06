package com.apptank.horus.client.sync.tasks

import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ValidateHashingTaskTest : TestCase() {

    @Mock
    val controlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    private lateinit var task: ValidateHashingTask

    @Before
    fun setup() {
        task = ValidateHashingTask(
            controlDatabaseHelper,
            synchronizationService
        )
    }

    @Test
    fun `when status hashing validated is completed then return success`() = runBlocking {
        // Given
        every { controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION) }
            .returns(true)

        // When
        val result = task.execute(null)

        // Then
        assert(result is TaskResult.Success)
        coVerify { synchronizationService.postValidateHashing(any()) }.wasNotInvoked()
    }

    @Test
    fun `when status hashing validate is not complete then validate hashing failure`() =
        runBlocking {
            // Given
            every {
                controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION)
            }.returns(false)

            coEvery { synchronizationService.postValidateHashing(any()) }
                .returns(DataResult.Failure(Exception("Hashing validation failed")))

            // When
            val result = task.execute(null)

            // Then
            assert(result is TaskResult.Failure)
        }

    @Test
    fun `when status hashing validate is not complete then validate hashing success and matched is false`() =
        runBlocking {
            // Given
            every {
                controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION)
            }.returns(false)

            coEvery { synchronizationService.postValidateHashing(any()) }
                .returns(
                    DataResult.Success(
                        SyncDTO.Response.HashingValidation(
                            randomHash(),
                            randomHash(),
                            false
                        )
                    )
                )

            // When
            val result = task.execute(null)

            // Then
            assert(result is TaskResult.Failure)
        }

    @Test
    fun `when status hashing validate is not complete then validate hashing success and matched is true`(): Unit =
        runBlocking {
            // Given
            every {
                controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION)
            }.returns(false)

            coEvery { synchronizationService.postValidateHashing(any()) }
                .returns(
                    DataResult.Success(
                        SyncDTO.Response.HashingValidation(
                            randomHash(),
                            randomHash(),
                            true
                        )
                    )
                )

            // When
            val result = task.execute(null)

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