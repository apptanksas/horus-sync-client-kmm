package com.apptank.horus.client.database.builder

import com.apptank.horus.client.migration.database.convertToSQL
import com.apptank.horus.client.migration.domain.Attribute


/**
 * Builder class for creating SQL statements to create database tables.
 */
class CreateTableSQLBuilder {

    private var tableName: String? = null
    private var attributes = mutableListOf<Attribute>()

    /**
     * Set the name of the table.
     * @param tableName The name of the table.
     * @return Instance of CreateTableSQLBuilder for method chaining.
     */
    fun setTableName(tableName: String): CreateTableSQLBuilder {
        this.tableName = tableName
        return this
    }

    /**
     * Add an attribute to the table.
     * @param attribute The attribute to be added to the table.
     * @return Instance of CreateTableSQLBuilder for method chaining.
     */
    fun addAttribute(attribute: Attribute): CreateTableSQLBuilder {
        attributes.add(attribute)
        return this
    }

    /**
     * Build the SQL statement to create the table.
     * @return SQL statement to create the table.
     * @throws IllegalArgumentException if tableName is null.
     */
    fun build(): String {
        tableName ?: throw IllegalArgumentException("TableName is missing")
        return "CREATE TABLE IF NOT EXISTS $tableName (" + attributes.joinToString(", ") { it.convertToSQL() } + ")"
    }

}