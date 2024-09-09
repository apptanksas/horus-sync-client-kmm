package com.apptank.horus.client.database

import com.apptank.horus.client.data.Horus
import kotlin.random.Random

sealed class SQL {

    data class ColumnValue(
        val column: String,
        val value: Any
    )

    data class WhereCondition(
        val columnValue: ColumnValue,
        val comparator: Comparator = Comparator.EQUALS
    )

    /**
     * Enum class representing logical operators to be used in combining conditions.
     */
    enum class LogicOperator {
        AND,
        OR
    }

    enum class Comparator(val value: String) {
        EQUALS("="),
        NOT_EQUALS("!="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUALS(">="),
        LESS_THAN_OR_EQUALS("<=")
    }

    data class OperatorKey(
        val operator: LogicOperator,
        val id: Int = Random.nextInt()
    )

    enum class OrderBy {
        ASC,
        DESC
    }

}


fun Horus.Attribute<*>.toDBColumnValue(): SQL.ColumnValue {
    return SQL.ColumnValue(name, value!!)
}

fun List<Horus.Attribute<*>>.mapToDBColumValue(): List<SQL.ColumnValue> {
    return this.map { it.toDBColumnValue() }
}