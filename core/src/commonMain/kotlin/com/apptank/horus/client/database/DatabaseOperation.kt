package com.apptank.horus.client.database


/**
 * Base class representing an action to be performed on a database table.
 *
 * @property table The name of the database table.
 */

sealed class DatabaseOperation(open val table: String){

    /**
     * Data class representing an insert action to be performed on a database table.
     *
     * @property table The name of the database table.
     * @property values A list of column-value pairs to be inserted.
     */
    data class InsertRecord(
        override val table: String,
        val values: List<SQL.ColumnValue>
    ) : DatabaseOperation(table)

    /**
     * Data class representing an update operation on a database record.
     *
     * @property table The name of the table.
     * @property values The list of column-value pairs to be updated.
     * @property conditions The list of conditions for the update.
     * @property operator The logical operator to combine conditions (AND/OR).
     */
    data class UpdateRecord(
        override val table: String,
        val values: List<SQL.ColumnValue>,
        val conditions: List<SQL.WhereCondition>,
        val operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ) : DatabaseOperation(table)


    /**
     * Data class representing a delete action to be performed on a database table.
     *
     * @property table The name of the database table.
     * @property values A list of column-value pairs to be used as conditions for deletion.
     */
    data class DeleteRecord(
        override val table: String,
        val conditions: List<SQL.WhereCondition>,
        val operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ) : DatabaseOperation(table)
}