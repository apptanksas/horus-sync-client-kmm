package org.apptank.horus.client.lifecycle

import org.apptank.horus.client.base.CallbackEvent
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.ILifeCycle
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.bus.InternalEventBus
import org.apptank.horus.client.bus.EventType
import org.apptank.horus.client.sync.manager.DispenserManager
import org.apptank.horus.client.sync.manager.ISyncFileUploadedManager
import org.apptank.horus.client.tasks.ControlTaskManager


object HorusLifeCycle : ILifeCycle {

    private val dispenserManager: DispenserManager by lazy { HorusContainer.getDispenserManager() }
    private val syncFileUploadedManager: ISyncFileUploadedManager by lazy { HorusContainer.getSyncFileUploadedManager() }

    private var callbackEventActionCreated: CallbackEvent = {
        dispenserManager.processBatch()
    }

    private var callbackSetupChanged: CallbackEvent = {
        ControlTaskManager.start()
    }

    private val networkValidator: INetworkValidator by lazy { HorusContainer.getNetworkValidator() }

    override fun onCreate() = Unit

    override fun onResume() {

        with(InternalEventBus) {
            register(EventType.ACTION_CREATED, callbackEventActionCreated)
            register(EventType.SETUP_CHANGED, callbackSetupChanged)
        }

        networkValidator.registerNetworkCallback()
        ControlTaskManager.start()
        syncFileUploadedManager.syncFiles()
    }

    override fun onPause() {
        with(InternalEventBus) {
            unregister(EventType.ACTION_CREATED, callbackEventActionCreated)
            unregister(EventType.SETUP_CHANGED, callbackSetupChanged)
        }
        networkValidator.unregisterNetworkCallback()
    }

}