package com.apptank.horus.client.database

import com.apptank.horus.client.data.Horus
import kotlin.random.Random

sealed class SQL {

    /**
     * Data class representing a column-value pair in a database table.
     *
     * @property column The name of the column.
     * @property value The value associated with the column.
     */
    data class ColumnValue(
        val column: String,
        val value: Any
    )

    /**
     * Data class representing a condition to be used in database operations.
     *
     * @property columnValue The column-value pair to be compared.
     * @property comparator The comparison operator to be used (e.g., '=', '!=', '<', '>').
     */
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