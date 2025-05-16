package org.apptank.horus.client.extensions

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlin.math.abs

fun Instant.diffInHours(other: Instant, timeZone: TimeZone = TimeZone.UTC): Int {
    return abs(this.periodUntil(other, timeZone).hours)
}

fun Instant.diffInHoursFromNow(timeZone: TimeZone = TimeZone.UTC): Int {
    return this.diffInHours(Clock.System.now(), timeZone)
}