package com.apptank.horus.client.data

data class Condition(
    val column: String,
    val value: Any,
    val operator: String = "="
)