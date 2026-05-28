package org.apptank.horus.client.database.builder

import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.extensions.prepareSQLValueAsString
import kotlin.math.cos

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

    protected var wrapExists = false

    private var extensions = mutableListOf<SQL.Extension>()

    private var conditions =
        mutableMapOf<SQL.OperatorKey, Pair<SQL.LogicOperator, List<SQL.WhereCondition>>>()

    private var limit: Int? = null

    private var offset: Int? = null

    private var orderBy: MutableList<Pair<String, SQL.OrderBy>>? = null

    /**
     * Adds `WHERE` conditions to the query with an `AND` join operator by default.
     *
     * @param condition The conditions to add.
     * @param joinOperator The logic operator to use when joining multiple conditions (default is `AND`).
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    open fun where(
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
    open fun whereOr(
        vararg condition: SQL.WhereCondition,
        joinOperator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): QueryBuilder {
        return addWhere(joinOperator, SQL.LogicOperator.OR, *condition)
    }

    /**
     * Adds an `IN` condition to the query.
     *
     * @param column The column to filter by.
     * @param values The list of values to filter by.
     * @param joinOperator The logic operator to use when joining multiple conditions (default is `AND`).
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    open fun whereIn(
        column: String,
        values: List<Any>,
        joinOperator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): QueryBuilder {
        val condition = SQL.WhereCondition(
            SQL.ColumnValue(column, values),
            SQL.Comparator.IN
        )
        return addWhere(joinOperator, SQL.LogicOperator.AND, condition)
    }

    /**
     * Specifies the attributes (columns) to select in the query.
     *
     * @param attributes The list of attributes to select.
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    open fun select(vararg attributes: String): QueryBuilder {
        attributeSelection.addAll(attributes)
        return this
    }

    /**
     * Specifies that the query should return the count of records.
     *
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    abstract fun selectCount(): QueryBuilder

    /**
     * Sets the limit on the number of results returned by the query.
     *
     * @param limit The maximum number of results to return.
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    open fun limit(limit: Int): QueryBuilder {
        this.limit = limit
        return this
    }

    /**
     * Sets the offset for the results returned by the query.
     *
     * @param offset The number of results to skip before starting to return results.
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    open fun offset(offset: Int): QueryBuilder {
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
    open fun orderBy(column: String, orderBy: SQL.OrderBy = SQL.OrderBy.DESC): QueryBuilder {
        if (this.orderBy == null) {
            this.orderBy = mutableListOf()
        }
        this.orderBy?.add(Pair(column, orderBy))
        return this
    }

    /**
     * Adds an extension to the query.
     *
     * @param extension The extension to add.
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    open fun withExtension(extension: SQL.Extension): QueryBuilder {
        extensions.add(extension)
        return this
    }

    /**
     * Wraps the query in a SELECT EXISTS(...) statement.
     *
     * @return The current instance of [QueryBuilder] for method chaining.
     */
    open fun asExists(): QueryBuilder {
        this.wrapExists = true
        return this
    }

    /**
     * Returns the list of extensions added to the query.
     *
     * @return A list of [SQL.Extension].
     */
    protected fun getExtensions(): List<SQL.Extension> {
        return extensions
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

            conditionGrouped += conditions.second.joinToString(
                " ${conditions.first.name} ",
                transform = {
                    if (it.comparator == SQL.Comparator.IS_NULL || it.comparator == SQL.Comparator.IS_NOT_NULL) {
                        return@joinToString "${it.columnValue.column} ${it.comparator.value}"
                    }
                    "${it.columnValue.column} ${it.comparator.value} ${it.columnValue.value.prepareSQLValueAsString()}"
                })

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
        var base = " ORDER BY"

        orderBy?.forEach {
            base += " ${it.first} ${it.second.name},"
        }

        return base.removeSuffix(",")
    }

    /**
     * Builds the coordinate extension filter for the query.
     * Uses a bounding box approximation for filtering points within a distance.
     *
     * The formula uses the fact that:
     * - 1 degree of latitude ≈ 111.32 km
     * - 1 degree of longitude ≈ 111.32 * cos(latitude) km
     *
     * @return The WHERE clause fragment for coordinate filtering.
     */
    protected fun buildCoordinateExtension(): String {
        val coordinateExtension =
            getExtensions().filterIsInstance<SQL.Coordinates.WithIn>().firstOrNull() ?: return ""

        val column = coordinateExtension.column
        val refLat = coordinateExtension.point.latitude
        val refLon = coordinateExtension.point.longitude
        val distanceKm = coordinateExtension.distanceInKm

        // Earth's radius in km
        val earthRadiusKm = 6371.0

        // Calculate the delta for latitude and longitude based on distance
        val deltaLat = distanceKm / earthRadiusKm * (180.0 / kotlin.math.PI)
        val refLatRadians = refLat * kotlin.math.PI / 180.0
        val deltaLon = distanceKm / (earthRadiusKm * cos(refLatRadians)) * (180.0 / kotlin.math.PI)

        // Calculate bounding box
        val minLat = refLat - deltaLat
        val maxLat = refLat + deltaLat
        val minLon = refLon - deltaLon
        val maxLon = refLon + deltaLon

        // SQL expressions to extract latitude and longitude from "lat,lon" format
        val latExpr = "CAST(SUBSTR($column, 1, INSTR($column, ',') - 1) AS REAL)"
        val lonExpr = "CAST(SUBSTR($column, INSTR($column, ',') + 1) AS REAL)"

        // Build the WHERE clause using bounding box
        val wherePrefix = if (buildWhere().isEmpty()) " WHERE " else " AND "

        return "$wherePrefix$latExpr >= $minLat AND $latExpr <= $maxLat AND $lonExpr >= $minLon AND $lonExpr <= $maxLon"
    }

    /**
     * Returns a list of tables used in the query.
     *
     * @return A list of table names.
     */
    abstract fun getTables(): List<String>

    /**
     * Constructs the final SQL query string.
     *
     * @return The complete SQL query string.
     */
    abstract fun build(): String

    /**
     * Wraps the given SQL query in a SELECT EXISTS(...) statement if wrapExists is true.
     *
     * @param sql The SQL query to wrap.
     * @return The wrapped SQL query or the original query.
     */
    protected fun wrapInExists(sql: String): String {
        return if (wrapExists) "SELECT EXISTS($sql)" else sql
    }
}