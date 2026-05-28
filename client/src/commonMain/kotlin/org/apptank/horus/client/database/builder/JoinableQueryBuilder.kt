package org.apptank.horus.client.database.builder

import org.apptank.horus.client.database.struct.SQL

/**
 * A query builder that supports SQL joins between multiple tables.
 *
 * This class extends [QueryBuilder] and provides methods to perform different types of joins,
 * such as INNER JOIN and LEFT JOIN.
 *
 * Example usage:
 * ```
 * val sql = JoinableQueryBuilder("users")
 *     .select("users.name", "profiles.bio")
 *     .join("profiles", "users.id = profiles.user_id", SQL.JoinType.INNER)
 *     .where(SQL.WhereCondition(SQL.ColumnValue("users.status", "active")))
 *     .build()
 * ```
 *
 * @param mainTable The name of the primary table for the query.
 */
class JoinableQueryBuilder(
    private val mainTable: String
) : QueryBuilder() {

    private val joins = mutableListOf<JoinInfo>()

    private var selectCount = false

    init {
        if (mainTable.isEmpty()) {
            throw IllegalArgumentException("mainTable cannot be empty")
        }
    }

    /**
     * Internal representation of a Join clause.
     */
    data class JoinInfo(
        val type: SQL.JoinType,
        val table: String,
        val on: String
    )

    /**
     * Specifies that the query should return the count of records.
     *
     * @return The current instance of [JoinableQueryBuilder] for method chaining.
     */
    fun selectCount(): JoinableQueryBuilder {
        selectCount = true
        return this
    }

    /**
     * Adds a join clause to the query.
     *
     * @param table The table to join with.
     * @param on The condition for the join (e.g., "table1.id = table2.id").
     * @param type The type of join (default is [SQL.JoinType.INNER]).
     * @return The current instance of [JoinableQueryBuilder] for method chaining.
     */
    fun join(table: String, on: String, type: SQL.JoinType = SQL.JoinType.INNER): JoinableQueryBuilder {
        joins.add(JoinInfo(type, table, on))
        return this
    }

    /**
     * Adds an INNER JOIN clause to the query.
     *
     * @param table The table to join with.
     * @param on The condition for the join.
     * @return The current instance of [JoinableQueryBuilder] for method chaining.
     */
    fun innerJoin(table: String, on: String): JoinableQueryBuilder {
        return join(table, on, SQL.JoinType.INNER)
    }

    /**
     * Adds a LEFT JOIN clause to the query.
     *
     * @param table The table to join with.
     * @param on The condition for the join.
     * @return The current instance of [JoinableQueryBuilder] for method chaining.
     */
    fun leftJoin(table: String, on: String): JoinableQueryBuilder {
        return join(table, on, SQL.JoinType.LEFT)
    }

    override fun select(vararg attributes: String): JoinableQueryBuilder {
        super.select(*attributes)
        return this
    }

    override fun where(
        vararg condition: SQL.WhereCondition,
        joinOperator: SQL.LogicOperator
    ): JoinableQueryBuilder {
        super.where(*condition, joinOperator = joinOperator)
        return this
    }

    override fun whereOr(
        vararg condition: SQL.WhereCondition,
        joinOperator: SQL.LogicOperator
    ): JoinableQueryBuilder {
        super.whereOr(*condition, joinOperator = joinOperator)
        return this
    }

    override fun whereIn(
        column: String,
        values: List<Any>,
        joinOperator: SQL.LogicOperator
    ): JoinableQueryBuilder {
        super.whereIn(column, values, joinOperator = joinOperator)
        return this
    }

    override fun limit(limit: Int): JoinableQueryBuilder {
        super.limit(limit)
        return this
    }

    override fun offset(offset: Int): JoinableQueryBuilder {
        super.offset(offset)
        return this
    }

    override fun orderBy(column: String, orderBy: SQL.OrderBy): JoinableQueryBuilder {
        super.orderBy(column, orderBy)
        return this
    }

    override fun withExtension(extension: SQL.Extension): JoinableQueryBuilder {
        super.withExtension(extension)
        return this
    }

    override fun getTables(): List<String> {
        return listOf(mainTable) + joins.map { it.table }
    }

    override fun build(): String {
        // Default to selecting all columns
        var selection = "*"

        // If specific attributes are selected, join them into a comma-separated string
        if (attributeSelection.isNotEmpty()) {
            selection = attributeSelection.joinToString(",")
        }

        if (selectCount) {
            selection = "COUNT(*)"
        }

        // Build the base SQL query with the selected columns and the main table
        val base = StringBuilder("SELECT $selection FROM $mainTable")

        // Append JOIN clauses
        joins.forEach { join ->
            base.append(" ${join.type.value} ${join.table} ON ${join.on}")
        }

        // Append standard clauses from QueryBuilder
        base.append(buildWhere())
        base.append(buildCoordinateExtension())
        base.append(buildOrderBy())
        base.append(buildLimit())
        base.append(buildOffset())

        // Return the final query string trimmed
        return wrapInExists(base.toString().trim())
    }
}
