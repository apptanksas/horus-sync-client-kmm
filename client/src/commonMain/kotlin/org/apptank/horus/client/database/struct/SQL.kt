package org.apptank.horus.client.database.struct

import org.apptank.horus.client.data.Horus
import kotlin.random.Random

/**
 * A sealed class representing SQL components for building queries and operations.
 */
sealed class SQL {

    /**
     * Represents a column-value pair for SQL operations.
     *
     * @property column The name of the column.
     * @property value The value associated with the column.
     */
    data class ColumnValue(
        val column: String,
        val value: Any?
    )

    /**
     * Represents a condition for SQL queries, including the column, value, and comparator.
     *
     * @property columnValue The column and value for the condition.
     * @property comparator The comparator used for the condition (default is [Comparator.EQUALS]).
     */
    data class WhereCondition(
        val columnValue: ColumnValue,
        val comparator: Comparator = Comparator.EQUALS
    )

    /**
     * Enum class representing logical operators used in SQL queries.
     */
    enum class LogicOperator {
        AND,
        OR
    }

    /**
     * Enum class representing comparators used in SQL conditions.
     *
     * @property value The string representation of the comparator.
     */
    enum class Comparator(val value: String) {
        EQUALS("="),
        NOT_EQUALS("!="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUALS(">="),
        LESS_THAN_OR_EQUALS("<="),
        IN("IN"),
    }

    /**
     * Represents a key for logical operators, including an optional unique identifier.
     *
     * @property operator The logical operator.
     * @property id A unique identifier for the operator key (default is a random integer).
     */
    data class OperatorKey(
        val operator: LogicOperator,
        val id: Int = Random.nextInt()
    )

    /**
     * Enum class representing sorting order in SQL queries.
     */
    enum class OrderBy {
        ASC,
        DESC
    }

}

/**
 * Converts a [Horus.Attribute] to an [SQL.ColumnValue].
 *
 * @return An [SQL.ColumnValue] representation of the attribute.
 */
fun Horus.Attribute<*>.toDBColumnValue(): SQL.ColumnValue {
    return SQL.ColumnValue(name, value)
}

/**
 * Maps a list of [Horus.Attribute] to a list of [SQL.ColumnValue].
 *
 * @return A list of [SQL.ColumnValue] derived from the attributes.
 */
fun List<Horus.Attribute<*>>.mapToDBColumValue(): List<SQL.ColumnValue> {
    return this.map { it.toDBColumnValue() }
}
