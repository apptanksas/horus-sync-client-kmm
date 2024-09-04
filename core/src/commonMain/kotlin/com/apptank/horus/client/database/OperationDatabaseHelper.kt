package com.apptank.horus.client.database

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.database.builder.QueryBuilder
import com.apptank.horus.client.exception.DatabaseOperationFailureException
import com.apptank.horus.client.extensions.log
import horus.HorusDatabase

internal class OperationDatabaseHelper(
    private val database: HorusDatabase,
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName), IOperationDatabaseHelper {


    /**
     * Executes a list of database operations (insert, update, delete) within a transaction.
     *
     * @param actions The list of actions to be performed on the database.
     * @return True if the transaction was successful, false otherwise.
     */
    override fun executeOperations(actions: List<DatabaseOperation>) = executeTransaction { _ ->
        actions.forEach { action ->
            val operationIsFailure: Boolean = when (action) {
                // Insert
                is DatabaseOperation.InsertRecord -> {
                    insertOrThrow(
                        action.table,
                        action.values.prepareMap()
                    )
                    false // Insert is always success
                }
                // Update
                is DatabaseOperation.UpdateRecord -> executeUpdate(
                    action.table,
                    action.values,
                    action.conditions,
                    action.operator
                ).isFailure
                // Delete
                is DatabaseOperation.DeleteRecord -> executeDelete(
                    action.table,
                    action.conditions,
                    action.operator
                ).isFailure

                else -> throw IllegalStateException("Action not supported")
            }

            if (operationIsFailure) {
                throw DatabaseOperationFailureException("Operation database is failed")
            }
        }
    }

    /**
     * Executes a variable number of database operations (insert, update, delete) within a transaction.
     *
     * @param actions The vararg of actions to be performed on the database.
     * @return True if the transaction was successful, false otherwise.
     */
    override fun executeOperations(vararg actions: DatabaseOperation) =
        executeOperations(actions.toList())

    /**
     * Inserts multiple records into the database within a transaction.
     *
     * @param records A list of records to be inserted.
     * @return True if the transaction was successful, false otherwise.
     */
    override fun insertWithTransaction(records: List<DatabaseOperation.InsertRecord>, postOperation: () -> Unit) =
        executeTransaction { db ->
            records.forEach { item ->
                val values = item.values.prepareMap()
                log("[Insert] Table: ${item.table} Values: $values")
                insertOrThrow(item.table, values)
            }
            postOperation()
        }

    /**
     * Updates multiple records in the database within a transaction.
     *
     * @param records A list of records to be updated.
     * @return True if the transaction was successful, false otherwise.
     */
    override fun updateWithTransaction(
        records: List<DatabaseOperation.UpdateRecord>,
        postOperation: () -> Unit
    ) =
        executeTransaction { _ ->
            records.forEach { item ->
                if (executeUpdate(
                        item.table,
                        item.values,
                        item.conditions,
                        item.operator
                    ).isFailure
                ) {
                    throw IllegalStateException("Update records failed")
                }
            }
            postOperation()
        }

    /**
     * Deletes records from a specified table based on conditions.
     *
     * @param table The name of the table.
     * @param conditions The list of conditions for deletion.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @return The result of the operation.
     */
    override fun deleteRecords(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator
    ): LocalDatabase.OperationResult {
        return executeDelete(table, conditions, operator)
    }

    /**
     * Deletes multiple records from the database within a transaction.
     *
     * @param records A list of records to be deleted.
     * @return True if the transaction was successful, false otherwise.
     */
    override fun deleteWithTransaction(
        records: List<DatabaseOperation.DeleteRecord>,
        postOperation: () -> Unit
    ) =
        executeTransaction { _ ->
            records.forEach {
                if (executeDelete(it.table, it.conditions, it.operator).isFailure) {
                    throw IllegalStateException("Delete records failed")
                }
            }
            postOperation()
        }

    /**
     * Executes a query using the provided QueryBuilder and returns the results as a list of maps.
     * Each map represents a record, where the keys are the column names and the values are the corresponding values.
     *
     * @param builder the QueryBuilder used to build the SQL query.
     * @return a list of maps, each representing a record from the query result.
     */
    override fun queryRecords(builder: QueryBuilder): List<DataMap> {
        // Initialize an empty mutable list to store the query results
        val output = mutableListOf<Map<String, Any>>()
        queryResult(builder.build()) { cursor ->
            val map = mutableMapOf<String, Any>()
            cursor.values.forEach {
                map[it.column.name] = cursor.getValue(it.column.name)
            }
            output.add(map)
        }
        // Reverse the output list because the map of result to list the order in the inverse way
        return output.reversed()
    }

    /**
     * Executes a block of code within a database transaction.
     *
     * @param executeBody The code block to be executed.
     * @return True if the transaction was successful, false otherwise.
     */
    private fun executeTransaction(executeBody: (SqlDriver) -> Unit): Boolean {
        runCatching {
            database.transaction {
                executeBody(driver)
            }
            return true
        }.getOrElse {
            it.printStackTrace()
            return false
        }
    }

    /**
     * Executes a delete operation on the database.
     *
     * @param db The database instance.
     * @param table The name of the table.
     * @param conditions The list of conditions for deletion.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @return The result of the delete operation.
     */
    private fun executeDelete(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): LocalDatabase.OperationResult {

        if (conditions.isEmpty()) {
            throw IllegalArgumentException("conditions not can be empty")
        }

        val whereEvaluation = buildWhereEvaluation(conditions, operator)
        val result = delete(table, whereEvaluation)
        return LocalDatabase.OperationResult(result > 0, result.toInt())
    }


    /**
     * Executes an update operation on the database.
     *
     * @param db The database instance.
     * @param table The name of the table.
     * @param values The list of column-value pairs to be updated.
     * @param conditions The list of conditions for the update.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @return The result of the update operation.
     */
    private fun executeUpdate(
        table: String,
        values: List<SQL.ColumnValue>,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): LocalDatabase.OperationResult {
        val whereEvaluation = buildWhereEvaluation(conditions, operator)
        log("[Update] table: $table Values: $values Conditions: $whereEvaluation")

        val result = update(table, values.prepareMap(), whereEvaluation)
        return LocalDatabase.OperationResult(result > 0, result.toInt())
    }

}