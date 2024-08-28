package com.apptank.horus.client.database.builder

import com.apptank.horus.client.migration.database.convertToSQL
import com.apptank.horus.client.migration.domain.Attribute


class AlterTableSQLBuilder {

    private var tableName: String? = null
    private var attribute: Attribute? = null

    fun setTableName(tableName: String): AlterTableSQLBuilder {
        this.tableName = tableName
        return this
    }

    fun setAttribute(attribute: Attribute): AlterTableSQLBuilder {
        this.attribute = attribute
        return this
    }

    fun build(): String {
        tableName ?: throw IllegalArgumentException("TableName is missing")
        attribute ?: throw IllegalArgumentException("Attribute is missing")
        return "ALTER TABLE $tableName ADD COLUMN ${attribute?.convertToSQL()}"
    }

}