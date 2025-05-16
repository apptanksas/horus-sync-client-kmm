package org.apptank.horus.client.control.helper

import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.database.builder.QueryBuilder
import org.apptank.horus.client.database.builder.SimpleQueryBuilder

interface IOperationDatabaseHelper {
    /**
     * Executes a list of database operations (insert, update, delete) within a transaction.
     *
     * @param actions The list of actions to be performed on the database.
     * @param postOperation The callback to be executed after the operation.
     * @return True if the transaction was successful, false otherwise.
     */
    fun executeOperations(actions: List<DatabaseOperation>, postOperation: Callback = {}): Boolean

    /**
     * Executes a variable number of database operations (insert, update, delete) within a transaction.
     *
     * @param postOperation The callback to be executed after the operation.
     * @param actions The vararg of actions to be performed on the database.
     * @return True if the transaction was successful, false otherwise.
     */
    @Deprecated(
        "Use executeOperations(actions: List<DatabaseOperation>, postOperation: Callback) instead, because not has postOperation parameter.",
        ReplaceWith("executeOperations(actions.toList(), postOperation)")
    )
    fun executeOperations(vararg actions: DatabaseOperation): Boolean

    /**
     * Inserts multiple records into the database within a transaction.
     *
     * @param records A list of records to be inserted.
     * @return True if the transaction was successful, false otherwise.
     */
    fun insertWithTransaction(
        records: List<DatabaseOperation.InsertRecord>,
        postOperation: Callback = {}
    ): Boolean

    /**
     * Updates multiple records in the database within a transaction.
     *
     * @param records A list of records to be updated.
     * @return True if the transaction was successful, false otherwise.
     */
    fun updateWithTransaction(
        records: List<DatabaseOperation.UpdateRecord>,
        postOperation: Callback = {}
    ): Boolean

    /**
     * Deletes multiple records from the database within a transaction.
     *
     * @param records A list of records to be deleted.
     * @return True if the transaction was successful, false otherwise.
     */
    fun deleteWithTransaction(
        records: List<DatabaseOperation.DeleteRecord>,
        postOperation: Callback = {}
    ): Boolean

    /**
     * Deletes records from a specified table based on conditions.
     *
     * @param table The name of the table.
     * @param conditions The list of conditions for deletion.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @param disableForeignKeys Flag to disable foreign key checks during deletion.
     * @return The result of the operation.
     */
    fun deleteRecords(
        table: String,
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator = SQL.LogicOperator.AND,
        disableForeignKeys: Boolean = false
    ): DatabaseOperation.Result

    /**
     * Executes a query using the provided QueryBuilder and returns the results as a list of maps.
     * Each map represents a record, where the keys are the column names and the values are the corresponding values.
     *
     * @param builder the QueryBuilder used to build the SQL query.
     * @return a list of maps, each representing a record from the query result.
     */
    fun queryRecords(builder: QueryBuilder): List<DataMap>

    /**
     * Executes a query using the provided SimpleQueryBuilder and returns the count of records.
     * @param builder the SimpleQueryBuilder used to build the SQL query.
     *
     * @return the count of records from the query result.
     */
    fun countRecords(builder: SimpleQueryBuilder): Int


    /**
     * Truncates the specified entity from the database.
     *
     * @param entity The name of the entity to be truncated.
     * @return True if the truncation was successful, false otherwise.
     */
    fun truncate(entity: String)
}