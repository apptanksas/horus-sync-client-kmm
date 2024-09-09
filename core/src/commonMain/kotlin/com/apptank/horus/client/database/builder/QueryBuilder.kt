package com.apptank.horus.client.database.builder

import com.apptank.horus.client.database.SQL
import com.apptank.horus.client.extensions.prepareSQLValueAsString

abstract class QueryBuilder {

    protected var attributeSelection = mutableListOf<String>()

    private var conditions =
        mutableMapOf<SQL.OperatorKey, Pair<SQL.LogicOperator, List<SQL.WhereCondition>>>()

    private var limit: Int? = null

    private var offset: Int? = null

    private var orderBy: Pair<String, SQL.OrderBy>? = null

    fun where(
        vararg condition: SQL.WhereCondition,
        joinOperator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): QueryBuilder {
        return addWhere(joinOperator, SQL.LogicOperator.AND, *condition)
    }

    fun whereOr(
        vararg condition: SQL.WhereCondition,
        joinOperator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): QueryBuilder {
        return addWhere(joinOperator, SQL.LogicOperator.OR, *condition)
    }

    fun select(vararg attributes: String): QueryBuilder {
        attributeSelection.addAll(attributes)
        return this
    }

    fun limit(limit: Int): QueryBuilder {
        this.limit = limit
        return this
    }

    fun offset(offset: Int): QueryBuilder {
        this.offset = offset
        return this
    }

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

    protected fun buildLimit(): String {
        limit ?: return ""
        return " LIMIT $limit"
    }

    protected fun buildOffset(): String {
        offset ?: return ""
        return " OFFSET $offset"
    }

    protected fun buildOrderBy(): String {
        orderBy ?: return ""
        return " ORDER BY ${orderBy?.first} ${orderBy?.second?.name}"
    }


    abstract fun build(): String
}