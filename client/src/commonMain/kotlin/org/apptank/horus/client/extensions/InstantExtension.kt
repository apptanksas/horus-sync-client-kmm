package org.apptank.horus.client.extensions

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlin.math.abs

fun Instant.diffInHours(other: Instant): Int {
    val diffInSeconds = abs(this.epochSeconds - other.epochSeconds)
    return (diffInSeconds / 3600).toInt()
}

fun Instant.diffInHoursFromNow(): Int {
    return this.diffInHours(Clock.System.now())
}