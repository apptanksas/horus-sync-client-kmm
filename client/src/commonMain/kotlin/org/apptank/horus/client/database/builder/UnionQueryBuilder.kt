package org.apptank.horus.client.database.builder

import org.apptank.horus.client.database.struct.SQL

/**
 * A query builder that supports SQL UNION and UNION ALL between multiple subqueries.
 *
 * This class extends [QueryBuilder] and provides methods to combine multiple [QueryBuilder]
 * instances using UNION or UNION ALL operators.
 */
class UnionQueryBuilder : QueryBuilder() {

    private val queries = mutableListOf<QueryBuilder>()
    private var isUnionAll = true

    /**
     * Adds a subquery to the union.
     *
     * @param query The [QueryBuilder] instance representing the subquery.
     * @return The current instance of [UnionQueryBuilder] for method chaining.
     */
    fun add(query: QueryBuilder): UnionQueryBuilder {
        queries.add(query)
        return this
    }

    /**
     * Sets whether to use UNION ALL (default) or UNION (distinct results).
     *
     * @param value True for UNION ALL, false for UNION.
     * @return The current instance of [UnionQueryBuilder] for method chaining.
     */
    fun unionAll(value: Boolean = true): UnionQueryBuilder {
        isUnionAll = value
        return this
    }

    override fun select(vararg attributes: String): UnionQueryBuilder {
        super.select(*attributes)
        return this
    }

    override fun limit(limit: Int): UnionQueryBuilder {
        super.limit(limit)
        return this
    }

    override fun offset(offset: Int): UnionQueryBuilder {
        super.offset(offset)
        return this
    }

    override fun orderBy(column: String, orderBy: SQL.OrderBy): UnionQueryBuilder {
        super.orderBy(column, orderBy)
        return this
    }

    override fun asExists(): UnionQueryBuilder {
        super.asExists()
        return this
    }

    override fun getTables(): List<String> {
        return queries.flatMap { it.getTables() }.distinct()
    }

    override fun build(): String {
        if (queries.isEmpty()) {
            return ""
        }

        val operator = if (isUnionAll) " UNION ALL " else " UNION "
        val combinedQueries = queries.joinToString(operator) { it.build() }

        val base = StringBuilder(combinedQueries)

        // Append standard clauses from QueryBuilder if they apply to the entire union
        base.append(buildOrderBy())
        base.append(buildLimit())
        base.append(buildOffset())

        return wrapInExists(base.toString().trim())
    }
}
