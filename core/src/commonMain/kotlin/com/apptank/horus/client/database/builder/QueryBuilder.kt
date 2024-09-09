package com.apptank.horus.client.database.builder

import com.apptank.horus.client.database.SQL
import com.apptank.horus.client.extensions.prepareSQLValueAsString

/**
 * Abstract class for constructing SQL queries with support for selecting attributes, applying filters,
 * limiting results, and ordering the results.
 *
 * This class provides methods to build different parts of a SQL query, including the `SELECT`, `WHERE`,
 * `LIMIT`, `OFFSET`, and `ORDER BY` clauses. Subclasses should implement the `build` method to produce
 * the final SQL query string.
 *
 * Example usage:
 * ```
 * val sql = MyQueryBuilder()
 *     .select("id", "name")
 *     .where(SQL.WhereCondition("status", SQL.Comparator.EQUALS, "active"))
 *     .limit(10)
 *     .offset(20)
 *     .orderBy("name", SQL.OrderBy.ASC)
 *     .build()
 * ```
 *
 * This example will produce a SQL query similar to:
 * ```
 * SELECT id, name
 * FROM table_name
 * WHERE status = 'active'
 * ORDER BY name ASC
 * LIMIT 10 OFFSET 20
 * ```
 */
abstract class QueryBuilder {

    protected var attributeSelection = mutableListOf<String>()

    private var conditions =
        mutableMapOf<SQL.OperatorKey, Pair<SQL.LogicOperator, List<SQL.WhereCondition>>>()

    private var limit: Int? = null

    private var offset: Int? = null

    private var orderBy: Pair<String, SQL.OrderBy>? = null

    /**
     * Adds `WHERE` conditions to the query with an `AND` join operator by default.
     *
     * @param condition The conditions to add.
     * @param joinOperator The logic operator to use when joining multiple conditions (default is `AND`).
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    fun where(
        vararg condition: SQL.WhereCondition,
        joinOperator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): QueryBuilder {
        return addWhere(joinOperator, SQL.LogicOperator.AND, *condition)
    }

    /**
     * Adds `WHERE` conditions to the query with an `OR` join operator.
     *
     * @param condition The conditions to add.
     * @param joinOperator The logic operator to use when joining multiple conditions (default is `AND`).
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    fun whereOr(
        vararg condition: SQL.WhereCondition,
        joinOperator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): QueryBuilder {
        return addWhere(joinOperator, SQL.LogicOperator.OR, *condition)
    }

    /**
     * Specifies the attributes (columns) to select in the query.
     *
     * @param attributes The list of attributes to select.
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    fun select(vararg attributes: String): QueryBuilder {
        attributeSelection.addAll(attributes)
        return this
    }

    /**
     * Sets the limit on the number of results returned by the query.
     *
     * @param limit The maximum number of results to return.
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    fun limit(limit: Int): QueryBuilder {
        this.limit = limit
        return this
    }

    /**
     * Sets the offset for the results returned by the query.
     *
     * @param offset The number of results to skip before starting to return results.
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    fun offset(offset: Int): QueryBuilder {
        this.offset = offset
        return this
    }

    /**
     * Specifies the column and order to use for sorting the results.
     *
     * @param column The column to sort by.
     * @param orderBy The order direction (default is descending).
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    fun orderBy(column: String, orderBy: SQL.OrderBy = SQL.OrderBy.DESC): QueryBuilder {
        this.orderBy = Pair(column, orderBy)
        return this
    }

    private fun addWhere(
        joinOperator: SQL.LogicOperator,
        operatorCondition: SQL.LogicOperator,
        vararg condition: SQL.WhereCondition
    ): QueryBuilder {
        conditions[SQL.OperatorKey(joinOperator)] = Pair(operatorCondition, condition.toList())
        return this
    }

    /**
     * Builds the `WHERE` clause of the SQL query.
     *
     * @return The `WHERE` clause as a string.
     */
    protected open fun buildWhere(): String {
        var sentences = ""
        val hasGroups = conditions.size > 1

        if (conditions.isEmpty()) {
            return ""
        }

        conditions.onEachIndexed { index, entry ->
            val key = entry.key
            val conditions = entry.value

            if (hasGroups && index > 0) {
                sentences += " ${key.operator.name} "
            }

            // Open group
            var conditionGrouped = if (hasGroups) "(" else ""

            conditionGrouped += conditions.second.joinToString(" ${conditions.first.name} ",
                transform = { "${it.columnValue.column} ${it.comparator.value} ${it.columnValue.value.prepareSQLValueAsString()}" })

            if (hasGroups) {
                conditionGrouped += ")" // Close group
            }

            sentences += conditionGrouped
        }

        if (sentences.isBlank()) {
            return ""
        }

        return " WHERE ${sentences.trim()}"
    }

    /**
     * Builds the `LIMIT` clause of the SQL query.
     *
     * @return The `LIMIT` clause as a string.
     */
    protected fun buildLimit(): String {
        limit ?: return ""
        return " LIMIT $limit"
    }

    /**
     * Builds the `OFFSET` clause of the SQL query.
     *
     * @return The `OFFSET` clause as a string.
     */
    protected fun buildOffset(): String {
        offset ?: return ""
        return " OFFSET $offset"
    }

    /**
     * Builds the `ORDER BY` clause of the SQL query.
     *
     * @return The `ORDER BY` clause as a string.
     */
    protected fun buildOrderBy(): String {
        orderBy ?: return ""
        return " ORDER BY ${orderBy?.first} ${orderBy?.second?.name}"
    }

    /**
     * Constructs the final SQL query string.
     *
     * @return The complete SQL query string.
     */
    abstract fun build(): String
}