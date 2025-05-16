package org.apptank.horus.client.control.helper

import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.database.builder.QueryBuilder

interface IDataSharedDatabaseHelper {
    /**
     * Inserts one or more shared entity records into the database.
     *
     * Each record is converted to a JSON string and stored in the
     * configured table. This operation is executed within a single
     * transaction to ensure atomicity.
     *
     * @param records Vararg array of [SyncControl.EntityShared] records
     *        to insert.
     */
    fun insert(vararg records: SyncControl.EntityShared)
    /**
     * Removes all records from the shared data table.
     *
     * This method executes a DELETE statement directly on the driver,
     * clearing the table of all entries.
     */
    fun truncate()
    /**
     * Queries records from the shared data table based on the provided
     * [QueryBuilder] criteria.
     *
     * @param builder The [QueryBuilder] instance containing the query
     *        criteria.
     * @return A list of [DataMap] representing the queried records.
     */
    fun queryRecords(builder: QueryBuilder): List<DataMap>
}