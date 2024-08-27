package com.apptank.horus.client.base

sealed class DataResult<out T : Any?> {
    data class Success<out T : Any>(val data: T) : DataResult<T>()
    data class Failure(val exception: Throwable) : DataResult<Nothing>()
}

fun <R, T : Any> DataResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R
): R {
    return when (val result = this) {
        is DataResult.Success -> onSuccess(result.data)
        is DataResult.Failure -> onFailure(result.exception)
    }
}

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
    }
}