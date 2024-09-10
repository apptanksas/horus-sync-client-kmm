package com.apptank.horus.client.utils

import kotlinx.datetime.Clock

/**
 * Provides utility functions related to system time.
 */
internal object SystemTime {

    /**
     * Retrieves the current timestamp in seconds since the Unix epoch.
     *
     * @return The current timestamp as a [Long] value, representing the number of seconds since January 1, 1970, 00:00:00 GMT.
     */
    fun getCurrentTimestamp(): Long {
        return Clock.System.now().epochSeconds
    }
}
