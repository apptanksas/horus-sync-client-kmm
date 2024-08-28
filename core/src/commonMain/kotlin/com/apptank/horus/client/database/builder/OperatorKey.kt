package com.apptank.horus.client.database.builder

import com.apptank.horus.client.database.Operator
import kotlin.random.Random

data class OperatorKey(
    val operator: Operator,
    val id: Int = Random.nextInt()
)

enum class OrderBy {
    ASC,
    DESC
}