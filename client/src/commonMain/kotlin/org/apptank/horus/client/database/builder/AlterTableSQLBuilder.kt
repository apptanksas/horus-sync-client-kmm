package org.apptank.horus.client.database.builder

import org.apptank.horus.client.migration.database.convertToSQL
import org.apptank.horus.client.migration.domain.Attribute


/**
 * A builder class for constructing SQL `ALTER TABLE` statements.
 *
 * This class provides a fluent API for building SQL statements to alter a table by adding a new column.
 *
 * Example usage:
 * ```
 * val sql = AlterTableSQLBuilder()
 *     .setTableName("users")
 *     .setAttribute(Attribute("age", "INTEGER"))
 *     .build()
 * ```
 *
 * This example will produce the following SQL statement:
 * ```
 * ALTER TABLE users ADD COLUMN age INTEGER
 * ```
 */
class AlterTableSQLBuilder {

    private var tableName: String? = null
    private var attribute: Attribute? = null

    /**
     * Sets the name of the table to alter.
     *
     * @param tableName The name of the table.
     * @return The current instance of [AlterTableSQLBuilder] for method chaining.
     */
    fun setTableName(tableName: String): AlterTableSQLBuilder {
        this.tableName = tableName
        return this
    }

    /**
     * Sets the attribute to add as a new column in the table.
     *
     * @param attribute The attribute representing the new column.
     * @return The current instance of [AlterTableSQLBuilder] for method chaining.
     */
    fun setAttribute(attribute: Attribute): AlterTableSQLBuilder {
        this.attribute = attribute
        return this
    }

    /**
     * Builds the SQL `ALTER TABLE` statement.
     *
     * @return The constructed SQL statement as a string.
     * @throws IllegalArgumentException if tableName or attribute is missing.
     */
    fun build(): String {
        tableName ?: throw IllegalArgumentException("TableName is missing")
        attribute ?: throw IllegalArgumentException("Attribute is missing")
        return "ALTER TABLE $tableName ADD COLUMN ${attribute?.convertToSQL()}"
    }
}
