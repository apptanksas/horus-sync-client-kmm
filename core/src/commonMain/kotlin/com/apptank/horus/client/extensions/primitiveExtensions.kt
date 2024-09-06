package com.apptank.horus.client.extensions

fun Boolean?.isTrue(): Boolean {
    return this == true
}

fun Boolean?.isFalse(): Boolean {
    return this == false
}

fun <T> Boolean.evaluate(trueValue: T, falseValue: T): T {
    return if (this) trueValue else falseValue
}