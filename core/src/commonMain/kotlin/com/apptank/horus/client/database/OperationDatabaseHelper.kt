package com.apptank.horus.client.database

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.base.Callback
import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.database.builder.QueryBuilder
import com.apptank.horus.client.exception.DatabaseOperationFailureException
import com.apptank.horus.client.extensions.log

/**
 * Helper class for performing database operations, extending [SQLiteHelper] and implementing [IOperationDatabaseHelper].
 *
 * @param databaseName The name of the database.
 * @param driver The SQL driver used to interact with the database.
 */
internal class OperationDatabaseHelper(
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName), IOperationDatabaseHelper {

    /**
     * Executes a list of database operations in a transaction.
     *
     * @param actions List of [DatabaseOperation] to be executed.
     * @throws DatabaseOperationFailureException if any operation fails.
     */
    override fun executeOperations(actions: List<DatabaseOperation>) = executeTransaction { _ ->
        actions.forEach { action ->
            val operationIsFailure: Boolean = when (action) {
                // Insert operation
                is DatabaseOperation.InsertRecord -> {
                    insertOrThrow(
                        action.table,
                        action.values.prepareMap()
                    )
                    false // Insert is always considered successful
                }
                // Update operation
                is DatabaseOperation.UpdateRecord -> executeUpdate(
                    action.table,
                    action.values,
                    action.conditions,
                    action.operator
                ).isFailure
                // Delete operation
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
     * Executes a vararg list of database operations in a transaction.
     *
     * @param actions Vararg list of [DatabaseOperation] to be executed.
     * @throws DatabaseOperationFailureException if any operation fails.
     */
    override fun executeOperations(vararg actions: DatabaseOperation) =
        executeOperations(actions.toList())

    /**
     * Inserts records into the database within a transaction.
     *
     * @param records List of [DatabaseOperation.InsertRecord] to be inserted.
     * @param postOperation Callback to be executed after insertion.
     */
    override fun insertWithTransaction(
        records: List<DatabaseOperation.InsertRecord>,
        postOperation: Callback
    ) =
        executeTransaction { db ->
            records.forEach { item ->
                val values = item.values.prepareMap()
                log("[Insert] Table: ${item.table} Values: $values")
                insertOrThrow(item.table, values)
            }
            postOperation()
        }

    /**
     * Updates records in the database within a transaction.
     *
     * @param records List of [DatabaseOperation.UpdateRecord] to be updated.
     * @param postOperation Callback to be executed after update.
     * @throws IllegalStateException if any update operation fails.
     */
    override fun updateWithTransaction(
        records: List<DatabaseOperation.UpdateRecord>,
        postOperation: Callback
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
     * Deletes records from the database based on conditions.
     *
     * @param table The name of the table from which records will be deleted.
     * @param conditions List of [SQL.WhereCondition] to filter records for deletion.
     * @param operator Logic operator used to combine conditions.
     * @return A [DatabaseOperation.Result] indicating the result of the delete operation.
     */
    override fun deleteRecords(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator
    ): DatabaseOperation.Result {
        return executeDelete(table, conditions, operator)
    }

    /**
     * Deletes records from the database within a transaction.
     *
     * @param records List of [DatabaseOperation.DeleteRecord] to be deleted.
     * @param postOperation Callback to be executed after deletion.
     * @throws IllegalStateException if any delete operation fails.
     */
    override fun deleteWithTransaction(
        records: List<DatabaseOperation.DeleteRecord>,
        postOperation: Callback
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
     * Queries records from the database using a [QueryBuilder].
     *
     * @param builder A [QueryBuilder] used to construct the query.
     * @return A list of [DataMap] representing the query results.
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
     * Executes a transaction with the provided body function.
     *
     * @param executeBody A function to be executed within the transaction.
     * @return True if the transaction was successful, false otherwise.
     */
    private fun executeTransaction(executeBody: (SqlDriver) -> Unit): Boolean {
        return runCatching {
            transaction {
                executeBody(driver)
            }
            true
        }.getOrElse {
            it.printStackTrace()
            false
        }
    }

    /**
     * Executes a delete operation on the specified table with conditions.
     *
     * @param table The name of the table from which records will be deleted.
     * @param conditions List of [SQL.WhereCondition] to filter records for deletion.
     * @param operator Logic operator used to combine conditions.
     * @return A [DatabaseOperation.Result] indicating the result of the delete operation.
     * @throws IllegalArgumentException if conditions are empty.
     */
    private fun executeDelete(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): DatabaseOperation.Result {

        if (conditions.isEmpty()) {
            throw IllegalArgumentException("conditions not can be empty")
        }

        val whereEvaluation = buildWhereEvaluation(conditions, operator)
        val result = delete(table, whereEvaluation)
        return DatabaseOperation.Result(result > 0, result.toInt())
    }

    /**
     * Executes an update operation on the specified table with values and conditions.
     *
     * @param table The name of the table to update.
     * @param values List of [SQL.ColumnValue] to update.
     * @param conditions List of [SQL.WhereCondition] to filter records for update.
     * @param operator Logic operator used to combine conditions.
     * @return A [DatabaseOperation.Result] indicating the result of the update operation.
     */
    private fun executeUpdate(
        table: String,
        values: List<SQL.ColumnValue>,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ): DatabaseOperation.Result {
        val whereEvaluation = buildWhereEvaluation(conditions, operator)
        log("[Update] table: $table Values: $values Conditions: $whereEvaluation")

        val result = update(table, values.prepareMap(), whereEvaluation)
        return DatabaseOperation.Result(result > 0, result.toInt())
    }
}