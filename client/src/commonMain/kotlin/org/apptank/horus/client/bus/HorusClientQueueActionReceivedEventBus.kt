package org.apptank.horus.client.bus

import org.apptank.horus.client.control.SyncControl

object HorusClientQueueActionReceivedEventBus {

    private var listener: ((List<SyncControl.Action>) -> Unit)? = null

    fun register(listener: (List<SyncControl.Action>) -> Unit) {
        this.listener = listener
    }

    fun clear() {
        listener = null
    }

    fun emit(action: List<SyncControl.Action>) {
        runCatching { listener?.invoke(action) }
    }


}