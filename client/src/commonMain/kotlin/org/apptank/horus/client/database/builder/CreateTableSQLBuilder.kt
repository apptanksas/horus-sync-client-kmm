package org.apptank.horus.client.database.builder

import org.apptank.horus.client.migration.database.convertToSQL
import org.apptank.horus.client.migration.domain.Attribute


/**
 * A builder class for constructing SQL `CREATE TABLE` statements.
 *
 * This class provides a fluent API for building SQL statements to create a table with specified columns and constraints.
 *
 * Example usage:
 * ```
 * val sql = CreateTableSQLBuilder()
 *     .setTableName("users")
 *     .addAttribute(Attribute("id", "INTEGER PRIMARY KEY AUTOINCREMENT"))
 *     .addAttribute(Attribute("name", "TEXT NOT NULL"))
 *     .build()
 * ```
 *
 * This example will produce the following SQL statement:
 * ```
 * CREATE TABLE IF NOT EXISTS users (
 *     id INTEGER PRIMARY KEY AUTOINCREMENT,
 *     name TEXT NOT NULL
 * )
 * ```
 */
class CreateTableSQLBuilder {

    private var tableName: String? = null
    private var attributes = mutableListOf<Attribute>()

    /**
     * Sets the name of the table to create.
     *
     * @param tableName The name of the table.
     * @return The current instance of [CreateTableSQLBuilder] for method chaining.
     */
    fun setTableName(tableName: String): CreateTableSQLBuilder {
        this.tableName = tableName
        return this
    }

    /**
     * Adds an attribute (column) to the table.
     *
     * @param attribute The attribute representing a column to be added to the table.
     * @return The current instance of [CreateTableSQLBuilder] for method chaining.
     */
    fun addAttribute(attribute: Attribute): CreateTableSQLBuilder {
        attributes.add(attribute)
        return this
    }

    /**
     * Builds the SQL `CREATE TABLE` statement.
     *
     * @return The constructed SQL statement as a string.
     * @throws IllegalArgumentException if tableName is missing.
     */
    fun build(): String {
        tableName ?: throw IllegalArgumentException("TableName is missing")
        val constraints = mutableListOf<String>()
        var sqlOutput = "CREATE TABLE IF NOT EXISTS $tableName (" + attributes.joinToString(", ") {
            it.convertToSQL(
                applyConstraints = { constraints.addAll(it.map { it.sentence }) }
            )
        }
        // Add constraints to the SQL statement
        if (constraints.isNotEmpty()) {
            sqlOutput += ", " + constraints.joinToString(", ")
        }
        return "$sqlOutput)"
    }
}
