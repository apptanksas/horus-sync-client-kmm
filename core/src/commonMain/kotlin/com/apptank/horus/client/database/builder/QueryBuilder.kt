package com.apptank.horus.client.database.builder

import com.apptank.horus.client.database.LocalDatabase
import com.apptank.horus.client.extensions.prepareSQLValueAsString

/**
 * Abstract class representing a SQL query builder.
 * Provides methods to construct SQL queries with where conditions, select attributes, limit, and order by clauses.
 */
abstract class QueryBuilder {

    // List to store selected attributes
    protected var attributeSelection = mutableListOf<String>()
    // Map to store where conditions with their corresponding operators
    private var conditions = mutableMapOf<LocalDatabase.OperatorKey, Pair<LocalDatabase.OperatorComparator, List<LocalDatabase.WhereCondition>>>()
    // Variable to store the limit clause
    private var limit: Int? = null
    // Pair to store the order by clause
    private var orderBy: Pair<String, LocalDatabase.OrderBy>? = null

    /**
     * Adds a where condition with AND operator.
     *
     * @param condition the conditions to be added.
     * @param joinOperator the operator to join the conditions.
     * @return the QueryBuilder instance.
     */
    fun where(
        vararg condition: LocalDatabase.WhereCondition,
        joinOperator: LocalDatabase.OperatorComparator = LocalDatabase.OperatorComparator.AND
    ): QueryBuilder {
        return addWhere(joinOperator, LocalDatabase.OperatorComparator.AND, *condition)
    }

    /**
     * Adds a where condition with OR operator.
     *
     * @param condition the conditions to be added.
     * @param joinOperator the operator to join the conditions.
     * @return the QueryBuilder instance.
     */
    fun whereOr(
        vararg condition: LocalDatabase.WhereCondition,
        joinOperator: LocalDatabase.OperatorComparator = LocalDatabase.OperatorComparator.AND
    ): QueryBuilder {
        return addWhere(joinOperator, LocalDatabase.OperatorComparator.OR, *condition)
    }

    /**
     * Adds attributes to be selected in the query.
     *
     * @param attributes the attributes to be selected.
     * @return the QueryBuilder instance.
     */
    fun select(vararg attributes: String): QueryBuilder {
        attributeSelection.addAll(attributes)
        return this
    }

    /**
     * Sets the limit for the query.
     *
     * @param limit the maximum number of records to retrieve.
     * @return the QueryBuilder instance.
     */
    fun limit(limit: Int): QueryBuilder {
        this.limit = limit
        return this
    }

    /**
     * Sets the order by clause for the query.
     *
     * @param column the column to order by.
     * @param orderBy the order direction (ASC or DESC).
     * @return the QueryBuilder instance.
     */
    fun orderBy(column: String, orderBy: LocalDatabase.OrderBy = LocalDatabase.OrderBy.DESC): QueryBuilder {
        this.orderBy = Pair(column, orderBy)
        return this
    }

    /**
     * Adds where conditions to the query.
     *
     * @param joinOperator the operator to join different groups of conditions.
     * @param operatorCondition the operator to join individual conditions.
     * @param condition the conditions to be added.
     * @return the QueryBuilder instance.
     */
    private fun addWhere(
        joinOperator: LocalDatabase.OperatorComparator,
        operatorCondition: LocalDatabase.OperatorComparator,
        vararg condition: LocalDatabase.WhereCondition
    ): QueryBuilder {
        conditions[LocalDatabase.OperatorKey(joinOperator)] = Pair(operatorCondition, condition.toList())
        return this
    }

    /**
     * Builds the where clause for the query.
     *
     * @return the where clause as a string.
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
                transform = { "${it.columnValue.column} ${it.comparator} ${it.columnValue.value.prepareSQLValueAsString()}" })

            if (hasGroups) {
                conditionGrouped += ")" // Close group
            }

            sentences += conditionGrouped
        }

        return " WHERE ${sentences.trim()}"
    }

    /**
     * Builds the limit clause for the query.
     *
     * @return the limit clause as a string.
     */
    protected fun buildLimit(): String {
        limit ?: return ""
        return " LIMIT $limit"
    }

    /**
     * Builds the order by clause for the query.
     *
     * @return the order by clause as a string.
     */
    protected fun buildOrderBy(): String {
        orderBy ?: return ""
        return " ORDER BY ${orderBy?.first} ${orderBy?.second?.name}"
    }

    /**
     * Abstract method to build the final SQL query.
     *
     * @return the SQL query as a string.
     */
    abstract fun build(): String
}