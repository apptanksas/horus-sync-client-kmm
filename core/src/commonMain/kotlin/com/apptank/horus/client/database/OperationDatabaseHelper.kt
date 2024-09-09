package com.apptank.horus.client.database

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.base.Callback
import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.database.builder.QueryBuilder
import com.apptank.horus.client.exception.DatabaseOperationFailureException
import com.apptank.horus.client.extensions.log

internal class OperationDatabaseHelper(
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName), IOperationDatabaseHelper {

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

    override fun executeOperations(vararg actions: DatabaseOperation) =
        executeOperations(actions.toList())

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

    override fun deleteRecords(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator
    ): LocalDatabase.OperationResult {
        return executeDelete(table, conditions, operator)
    }

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

    private fun executeTransaction(executeBody: (SqlDriver) -> Unit): Boolean {
        runCatching {
            transaction {
                executeBody(driver)
            }
            return true
        }.getOrElse {
            it.printStackTrace()
            return false
        }
    }

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