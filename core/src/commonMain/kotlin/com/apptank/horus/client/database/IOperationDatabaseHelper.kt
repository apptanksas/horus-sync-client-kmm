package com.apptank.horus.client.database

import com.apptank.horus.client.database.builder.QueryBuilder

interface IOperationDatabaseHelper {
    /**
     * Executes a list of database operations (insert, update, delete) within a transaction.
     *
     * @param actions The list of actions to be performed on the database.
     * @return True if the transaction was successful, false otherwise.
     */
    fun executeOperations(actions: List<ActionDatabase>): Boolean

    /**
     * Executes a variable number of database operations (insert, update, delete) within a transaction.
     *
     * @param actions The vararg of actions to be performed on the database.
     * @return True if the transaction was successful, false otherwise.
     */
    fun executeOperations(vararg actions: ActionDatabase): Boolean

    /**
     * Inserts multiple records into the database within a transaction.
     *
     * @param records A list of records to be inserted.
     * @return True if the transaction was successful, false otherwise.
     */
    fun insertTransaction(records: List<RecordInsertData>, postOperation: () -> Unit = {}): Boolean

    /**
     * Updates multiple records in the database within a transaction.
     *
     * @param records A list of records to be updated.
     * @return True if the transaction was successful, false otherwise.
     */
    fun updateRecordTransaction(
        records: List<RecordUpdateData>,
        postOperation: () -> Unit = {}
    ): Boolean

    /**
     * Deletes multiple records from the database within a transaction.
     *
     * @param records A list of records to be deleted.
     * @return True if the transaction was successful, false otherwise.
     */
    fun deleteRecordTransaction(
        records: List<RecordDeleteData>,
        postOperation: () -> Unit = {}
    ): Boolean

    /**
     * Deletes records from a specified table based on conditions.
     *
     * @param table The name of the table.
     * @param conditions The list of conditions for deletion.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @return The result of the operation.
     */
    fun deleteRecord(
        table: String,
        conditions: List<WhereCondition>,
        operator: Operator = Operator.AND
    ): OperationResult

    /**
     * Executes a query using the provided QueryBuilder and returns the results as a list of maps.
     * Each map represents a record, where the keys are the column names and the values are the corresponding values.
     *
     * @param builder the QueryBuilder used to build the SQL query.
     * @return a list of maps, each representing a record from the query result.
     */
    fun queryRecords(builder: QueryBuilder): List<Map<String, Any>>
}