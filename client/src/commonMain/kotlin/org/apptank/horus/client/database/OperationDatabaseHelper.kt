package org.apptank.horus.client.database

import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.builder.QueryBuilder
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.exception.DatabaseOperationFailureException
import org.apptank.horus.client.extensions.getRequireBoolean
import org.apptank.horus.client.extensions.getRequireInt
import org.apptank.horus.client.extensions.handle
import org.apptank.horus.client.extensions.log
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.extensions.warn

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
     * @param postOperation Callback to be executed after the operations.
     * @throws DatabaseOperationFailureException if any operation fails.
     */
    override fun executeOperations(
        actions: List<DatabaseOperation>,
        ignoreIsFailure: Boolean,
        postOperation: Callback
    ) =
        executeTransaction { _ ->
            actions.forEach { action ->

                try {
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

                } catch (e: Exception) {

                    if (isForeignKeyConstraintFailure(e)) {
                        if (action is DatabaseOperation.DeleteRecord) {
                            warn("Delete operation failed due to foreign key constraint. Attempting cascade delete.")
                            executeDeleteOnCascade(
                                action.table,
                                action.conditions,
                                action.operator
                            )
                            return@forEach
                        }

                        if (isOperationRelatedToMissingForeignEntity(action)) {
                            warn("Operation skipped due to missing foreign key reference. Action: $action")
                            return@forEach
                        }
                    }

                    logException("Error processing action: $action", e)

                    if (!ignoreIsFailure) {
                        throw e
                    }
                }
            }
            // Execute the post operation callback
            postOperation()
        }


    override fun executeOperations(
        actions: List<DatabaseOperation>,
        postOperation: Callback
    ): Boolean {
        return executeOperations(actions, false, postOperation)
    }

    /**
     * Executes a vararg list of database operations in a transaction.
     *
     * @param actions Vararg list of [DatabaseOperation] to be executed.
     * @throws DatabaseOperationFailureException if any operation fails.
     */
    @Deprecated(
        "Use executeOperations(actions: List<DatabaseOperation>, postOperation: Callback) instead, because not has postOperation parameter.",
        replaceWith = ReplaceWith("executeOperations(actions.toList(), postOperation)")
    )
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
    ): Boolean {
        return executeTransaction { db ->
            records.forEachIndexed { index, item ->
                val values = item.values.prepareMap()
                insertOrThrow(item.table, values)
            }
            postOperation()
        }
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
     * @param disableForeignKeys Flag to disable foreign key checks during deletion.
     * @return A [DatabaseOperation.Result] indicating the result of the delete operation.
     */
    override fun deleteRecords(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator,
        disableForeignKeys: Boolean
    ): DatabaseOperation.Result {
        return executeDelete(table, conditions, operator, disableForeignKeys)
    }

    override fun executeDeleteOnCascade(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator
    ): DatabaseOperation.Result {

        if (conditions.isEmpty()) {
            throw IllegalArgumentException("conditions not can be empty")
        }

        var rowsAffected = 0
        val relationsByParent = getForeignKeysGroupedByParentTable()
        val operationIsSuccess = executeTransaction { _ ->
            rowsAffected = executeDeleteOnCascadeRecursive(
                table = table,
                conditions = conditions,
                operator = operator,
                relationsByParent = relationsByParent,
                processedScopes = mutableSetOf()
            )
        }

        return DatabaseOperation.Result(operationIsSuccess && rowsAffected > 0, rowsAffected)
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
     * Executes a query using the provided QueryBuilder and returns the count of records.
     * @param builder the QueryBuilder used to build the SQL query.
     *
     * @return the count of records from the query result.
     */
    override fun countRecords(builder: QueryBuilder): Int {
        val queryBuilder = builder.selectCount()
        var count = 0
        rawQuery(queryBuilder.build()) { cursor ->
            count = cursor.getRequireInt(0)
        }
        return count
    }


    /**
     * Executes a query using the provided QueryBuilder and returns whether any records exist.
     * @param builder the QueryBuilder used to build the SQL query.
     *
     * @return true if records exist, false otherwise.
     */
    override fun queryExists(builder: QueryBuilder): Boolean {
        val queryBuilder = builder.asExists()
        var exists = false
        rawQuery(queryBuilder.build()) { cursor ->
            exists = cursor.getRequireBoolean(0)
        }
        return exists
    }

    /**
     * Truncates the specified entity from the database.
     *
     * @param entity The name of the entity to be truncated.
     * @return True if the truncation was successful, false otherwise.
     */
    override fun truncate(entity: String) {
        driver.handle {
            executeDeleteWithNoForeignKeys("DELETE FROM $entity;")
        }
    }


    //------------------------------------------------------------------
    // PRIVATE METHODS
    //------------------------------------------------------------------

    private fun isForeignKeyConstraintFailure(exception: Exception): Boolean {
        return exception.message?.contains("FOREIGN KEY constraint failed", true) == true
    }

    private fun isOperationRelatedToMissingForeignEntity(action: DatabaseOperation): Boolean {
        return when (action) {
            is DatabaseOperation.InsertRecord -> hasMissingForeignKeyReference(action.table, action.values)
            is DatabaseOperation.UpdateRecord -> hasMissingForeignKeyReference(action.table, action.values)
            else -> false
        }
    }

    private fun hasMissingForeignKeyReference(
        table: String,
        values: List<SQL.ColumnValue>
    ): Boolean {
        if (values.isEmpty()) {
            return false
        }

        val valuesByColumn = values.associateBy { it.column }
        val relationsByConstraint = getForeignKeysByConstraint(table)

        relationsByConstraint.values.forEach { relationGroup ->
            val parentConditions = relationGroup.mapNotNull { relation ->
                val value = valuesByColumn[relation.childColumn]?.value ?: return@mapNotNull null

                SQL.WhereCondition(
                    SQL.ColumnValue(relation.parentColumn, value)
                )
            }

            if (parentConditions.size != relationGroup.size) {
                return@forEach
            }

            val whereEvaluation = buildWhereEvaluation(parentConditions, SQL.LogicOperator.AND)
            val parentTable = relationGroup.first().parentTable
            val parentExists = rawQuery("SELECT EXISTS(SELECT 1 FROM $parentTable WHERE $whereEvaluation)") {
                it.getRequireBoolean(0)
            }.firstOrNull() == true

            if (!parentExists) {
                return true
            }
        }

        return false
    }

    private fun getForeignKeysByConstraint(table: String): Map<Int, List<ForeignKeyRelation>> {
        val query = "PRAGMA foreign_key_list('$table')"
        return rawQuery(query) { cursor ->
            val parentTable = cursor.getString(2)
            val childColumn = cursor.getString(3)

            if (parentTable == null || childColumn == null) {
                null
            } else {
                ForeignKeyRelation(
                    constraintId = cursor.getRequireInt(0),
                    parentTable = parentTable,
                    childTable = table,
                    parentColumn = cursor.getString(4) ?: "id",
                    childColumn = childColumn
                )
            }
        }.groupBy { it.constraintId }
    }

    private fun executeDeleteOnCascadeRecursive(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator,
        relationsByParent: Map<String, List<ForeignKeyRelation>>,
        processedScopes: MutableSet<String>
    ): Int {
        val whereEvaluation = buildWhereEvaluation(conditions, operator)
        val scopeKey = "$table::$whereEvaluation"

        if (!processedScopes.add(scopeKey)) {
            return 0
        }

        var rowsAffected = 0

        val relationsGroupedByConstraint = relationsByParent[table]
            .orEmpty()
            .groupBy { "${it.childTable}::${it.constraintId}" }

        relationsGroupedByConstraint.values.forEach { relationGroup ->
            val parentColumns = relationGroup.map { it.parentColumn }.distinct()
            val parentRows = queryParentRowsByColumns(table, parentColumns, whereEvaluation)

            parentRows.forEach { parentRow ->
                val childConditions = relationGroup.map { relation ->
                    SQL.WhereCondition(
                        SQL.ColumnValue(
                            relation.childColumn,
                            parentRow[relation.parentColumn]
                        )
                    )
                }

                rowsAffected += executeDeleteOnCascadeRecursive(
                    table = relationGroup.first().childTable,
                    conditions = childConditions,
                    operator = SQL.LogicOperator.AND,
                    relationsByParent = relationsByParent,
                    processedScopes = processedScopes
                )
            }
        }

        rowsAffected += delete(table, whereEvaluation).toInt()
        return rowsAffected
    }

    private fun queryParentRowsByColumns(
        table: String,
        parentColumns: List<String>,
        whereEvaluation: String
    ): List<Map<String, Any?>> {
        val selectColumns = parentColumns.joinToString(", ")
        val query = "SELECT $selectColumns FROM $table WHERE $whereEvaluation"

        return queryResult(query) { cursor ->
            parentColumns.associateWith { parentColumn ->
                cursor.getValue<Any?>(parentColumn)
            }
        }
    }

    private fun getForeignKeysGroupedByParentTable(): Map<String, List<ForeignKeyRelation>> {
        val output = mutableMapOf<String, MutableList<ForeignKeyRelation>>()

        getTables().forEach { childTable ->
            val query = "PRAGMA foreign_key_list('$childTable')"
            val foreignKeys = rawQuery(query) { cursor ->
                val parentTable = cursor.getString(2)
                val childColumn = cursor.getString(3)

                if (parentTable == null || childColumn == null) {
                    null
                } else {
                    ForeignKeyRelation(
                        constraintId = cursor.getRequireInt(0),
                        parentTable = parentTable,
                        childTable = childTable,
                        parentColumn = cursor.getString(4) ?: "id",
                        childColumn = childColumn
                    )
                }
            }

            foreignKeys.forEach { relation ->
                output.getOrPut(relation.parentTable) { mutableListOf() }.add(relation)
            }
        }

        return output
    }


    /**
     * Executes a transaction with the provided body function.
     *
     * @param executeBody A function to be executed within the transaction.
     * @return True if the transaction was successful, false otherwise.
     */
    private fun executeTransaction(
        executeBody: (SqlDriver) -> Unit,
        onFailure: () -> Unit = {}
    ): Boolean {
        return runCatching {
            transaction {
                executeBody(driver)
            }
            true
        }.getOrElse {
            it.printStackTrace()
            onFailure()
            false
        }
    }

    private fun executeTransaction(executeBody: (SqlDriver) -> Unit): Boolean {
        return executeTransaction(executeBody) {
            logException("[Transaction] Transaction failed")
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
        operator: SQL.LogicOperator = SQL.LogicOperator.AND,
        disableForeignKeys: Boolean = false
    ): DatabaseOperation.Result {

        if (conditions.isEmpty()) {
            throw IllegalArgumentException("conditions not can be empty")
        }

        val whereEvaluation = buildWhereEvaluation(conditions, operator)
        val result = delete(table, whereEvaluation, disableForeignKeys)
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

        val result = update(table, values.prepareMap { column ->
            column
        }, whereEvaluation)
        return DatabaseOperation.Result(result > 0, result.toInt())
    }

    private data class ForeignKeyRelation(
        val constraintId: Int,
        val parentTable: String,
        val childTable: String,
        val parentColumn: String,
        val childColumn: String,
    )
}