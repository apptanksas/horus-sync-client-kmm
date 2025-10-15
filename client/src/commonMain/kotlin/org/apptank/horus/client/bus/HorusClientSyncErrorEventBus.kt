package org.apptank.horus.client.bus

object HorusClientSyncErrorEventBus {

    private val listeners = mutableListOf<(SyncError) -> Unit>()

    fun register(listener: (SyncError) -> Unit) {
        listeners.add(listener)
    }

    fun unregister(listener: (SyncError) -> Unit) {
        listeners.remove(listener)
    }

    fun emit(error: SyncError) {
        listeners.toList().forEach { listener ->
            listener(error)
        }
    }

    internal fun clear() {
        listeners.clear()
    }
}


sealed class SyncError {
    data class MaxCountEntityRestrictionExceeded(val entity: String, val maxCount: Int, val currentCount: Int) : SyncError()
    data class UnknownError(val error: Throwable?) : SyncError()
}


