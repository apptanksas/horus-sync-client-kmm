package com.apptank.horus.client

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.di.INetworkValidator
import com.apptank.horus.client.eventbus.CallbackEvent
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.sync.manager.RemoteSynchronizatorManager
import com.apptank.horus.client.tasks.ControlTaskManager

class HorusSynchronizeHandlerActivityLifeCycle : ActivityLifecycleCallbacks {

    private var remoteSynchronizatorManager: RemoteSynchronizatorManager? = null

    private var callbackEventActionCreated: CallbackEvent = {
        remoteSynchronizatorManager?.trySynchronizeData()
    }

    private val networkValidator: INetworkValidator by lazy { HorusContainer.getNetworkValidator() }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        remoteSynchronizatorManager = HorusContainer.createRemoteSynchronizatorManager().also {
            it.trySynchronizeData()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        ControlTaskManager.start()
    }

    override fun onActivityResumed(activity: Activity) {
        EventBus.register(EventType.ACTION_CREATED, callbackEventActionCreated)
        networkValidator.registerNetworkCallback()
    }

    override fun onActivityPaused(activity: Activity) {
        EventBus.unregister(EventType.ACTION_CREATED, callbackEventActionCreated)
        networkValidator.unregisterNetworkCallback()
    }

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

}