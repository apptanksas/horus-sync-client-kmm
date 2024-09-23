package org.apptank.horus.client.extensions

/**
 * Checks if the nullable Boolean value is `true`.
 *
 * @return `true` if the Boolean value is `true`; `false` otherwise. If the value is `null`, it returns `false`.
 */
fun Boolean?.isTrue(): Boolean {
    return this == true
}

/**
 * Checks if the nullable Boolean value is `false`.
 *
 * @return `true` if the Boolean value is `false`; `false` otherwise. If the value is `null`, it returns `false`.
 */
fun Boolean?.isFalse(): Boolean {
    return this == false
}

/**
 * Evaluates a Boolean value and returns one of the provided values based on the Boolean result.
 *
 * @param trueValue The value to return if the Boolean is `true`.
 * @param falseValue The value to return if the Boolean is `false`.
 * @return `trueValue` if the Boolean is `true`; `falseValue` if the Boolean is `false`. If the Boolean is `null`, it defaults to `falseValue`.
 */
fun <T> Boolean.evaluate(trueValue: T, falseValue: T): T {
    return if (this) trueValue else falseValue
}
