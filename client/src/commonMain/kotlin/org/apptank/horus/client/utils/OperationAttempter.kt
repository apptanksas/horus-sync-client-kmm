package org.apptank.horus.client.utils

import kotlinx.coroutines.delay
import org.apptank.horus.client.base.DataResult

object OperationAttempter {

    /**
     * Attempts to execute an operation that returns a `DataResult` with retry logic.
     *
     * This method retries the operation up to a maximum number of attempts if it returns a failure result. It includes a delay between retries.
     *
     * @param maxAttempts Maximum number of retry attempts before giving up.
     * @param callback A suspending function that performs the operation and returns a `DataResult`.
     * @return The result of the operation, which may be a success or failure.
     */
    suspend fun <T> attempt(
        maxAttempts: Int = 3,
        callback: suspend () -> DataResult<T>
    ): DataResult<T> {
        var attempts = 0L
        var result: DataResult<T>
        do {
            result = callback()
            if (attempts > 0) {
                delay(2000 * attempts)
            }
            attempts++
        } while (result is DataResult.Failure && attempts < maxAttempts)

        return result
    }
}