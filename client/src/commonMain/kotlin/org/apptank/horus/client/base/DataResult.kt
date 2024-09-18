package org.apptank.horus.client.base
/**
 * DataResult is a sealed class representing the result of an operation that can either succeed, fail, or result in unauthorized access.
 * It encapsulates the success data, failure with an exception, or an unauthorized error.
 *
 * @param T The type of data expected in the case of success.
 *
 * @sealed
 * @author John Ospina
 * @year 2024
 */
sealed class DataResult<out T : Any?> {

    /**
     * Success result containing the data of the operation.
     *
     * @param data The data returned by the operation.
     */
    data class Success<out T : Any>(val data: T) : DataResult<T>()

    /**
     * Failure result containing the exception that caused the failure.
     *
     * @param exception The exception thrown during the operation.
     */
    data class Failure(val exception: Throwable) : DataResult<Nothing>()

    /**
     * NotAuthorized result containing the exception that indicates unauthorized access.
     *
     * @param exception The exception related to unauthorized access.
     */
    data class NotAuthorized(val exception: Throwable) : DataResult<Nothing>()
}

/**
 * Synchronously processes the result of a DataResult by folding it into either a success or failure branch.
 *
 * @param R The type of the return value.
 * @param onSuccess A function to execute if the result is successful.
 * @param onFailure A function to execute if the result is a failure or unauthorized.
 * @return The result of executing the appropriate function.
 */
fun <R, T : Any> DataResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R
): R {
    return when (val result = this) {
        is DataResult.Success -> onSuccess(result.data)
        is DataResult.Failure -> onFailure(result.exception)
        is DataResult.NotAuthorized -> onFailure(result.exception)
    }
}

/**
 * Asynchronously processes the result of a DataResult using coroutines, allowing for suspension on success or failure.
 *
 * @param R The type of the return value.
 * @param onSuccess A suspending function to execute if the result is successful.
 * @param onFailure A suspending function to execute if the result is a failure or unauthorized.
 * @param onComplete A suspending function to execute when processing is complete, regardless of success or failure.
 * @return The result of executing the appropriate suspending function.
 */
suspend fun <R, T : Any> DataResult<T>.coFold(
    onSuccess: suspend (T) -> R,
    onFailure: suspend (Throwable) -> R,
    onComplete: suspend () -> Unit = {}
): R {
    return when (val result = this) {
        is DataResult.Success -> {
            onSuccess(result.data).also {
                onComplete.invoke()
            }
        }
        is DataResult.Failure -> {
            onFailure(result.exception).also {
                onComplete.invoke()
            }
        }
        is DataResult.NotAuthorized -> {
            onFailure(result.exception).also {
                onComplete.invoke()
            }
        }
    }
}
