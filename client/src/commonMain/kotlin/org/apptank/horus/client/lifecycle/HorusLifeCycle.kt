package org.apptank.horus.client.lifecycle

import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.ILifeCycle
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.eventbus.CallbackEvent
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.sync.manager.RemoteSynchronizatorManager
import org.apptank.horus.client.tasks.ControlTaskManager


object HorusLifeCycle: ILifeCycle {

    private var remoteSynchronizatorManager: RemoteSynchronizatorManager? = null

    private var callbackEventActionCreated: CallbackEvent = {
        remoteSynchronizatorManager?.trySynchronizeData()
    }

    private val networkValidator: INetworkValidator by lazy { HorusContainer.getNetworkValidator() }

    override fun onCreate() {
        remoteSynchronizatorManager = HorusContainer.createRemoteSynchronizatorManager().also {
            it.trySynchronizeData()
        }
    }

    override fun onResume() {
        EventBus.register(EventType.ACTION_CREATED, callbackEventActionCreated)
        networkValidator.registerNetworkCallback()
        ControlTaskManager.start()
    }

    override fun onPause() {
        EventBus.unregister(EventType.ACTION_CREATED, callbackEventActionCreated)
        networkValidator.unregisterNetworkCallback()
    }

}