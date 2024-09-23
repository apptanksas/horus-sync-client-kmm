package org.apptank.horus.client

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import org.apptank.horus.client.lifecycle.HorusLifeCycle

class HorusActivityLifeCycle : ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        HorusLifeCycle.onCreate()
    }

    override fun onActivityResumed(activity: Activity) {
        HorusLifeCycle.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        HorusLifeCycle.onPause()
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

}