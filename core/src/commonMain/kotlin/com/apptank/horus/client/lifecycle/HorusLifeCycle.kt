package com.apptank.horus.client.lifecycle

import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.di.ILifeCycle
import com.apptank.horus.client.di.INetworkValidator
import com.apptank.horus.client.eventbus.CallbackEvent
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.sync.manager.RemoteSynchronizatorManager
import com.apptank.horus.client.tasks.ControlTaskManager


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