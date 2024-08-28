package com.apptank.horus.client.utils

import kotlinx.datetime.Clock

object SystemTime {

    fun getCurrentTimestamp(): Long {
        return Clock.System.now().epochSeconds
    }
}