package org.apptank.horus.client.websocket

import org.apptank.horus.client.control.SyncControl

internal interface RealtimeSyncEventsSubscriber {
    suspend fun subscriber(ownerId: String, onActionReceived: (SyncControl.Action) -> Unit)
}