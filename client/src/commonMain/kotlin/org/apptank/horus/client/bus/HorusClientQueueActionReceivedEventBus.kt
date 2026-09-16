package org.apptank.horus.client.bus

import org.apptank.horus.client.control.SyncControl

object HorusClientQueueActionReceivedEventBus {

    private var listener: ((SyncControl.Action) -> Unit)? = null

    fun register(listener: (SyncControl.Action) -> Unit) {
        this.listener = listener
    }

    fun clear() {
        listener = null
    }

    fun emit(action: SyncControl.Action) {
        runCatching { listener?.invoke(action) }
    }


}