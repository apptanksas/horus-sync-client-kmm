package org.apptank.horus.client.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.base.encodeToJSON
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IDataSharedDatabaseHelper
import org.apptank.horus.client.control.scheme.DataSharedTable
import org.apptank.horus.client.database.builder.QueryBuilder
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.extensions.handle

/**
 * Helper class for managing shared data in a SQLite database.
 *
 * This class extends the generic [SQLiteHelper] and implements the
 * [IDataSharedDatabaseHelper] interface to provide CRUD operations
 * for shared data entities.
 *
 * @property databaseName The name of the SQLite database file.
 * @property driver Underlying SQL driver used for executing statements.
 */
class DataSharedDatabaseHelper(
    private val databaseName: String,
    driver: SqlDriver
) : SQLiteHelper(driver, databaseName), IDataSharedDatabaseHelper {

    /**
     * Inserts one or more shared entity records into the database.
     *
     * Each record is converted to a JSON string and stored in the
     * configured table. This operation is executed within a single
     * transaction to ensure atomicity.
     *
     * @param records Vararg array of [SyncControl.EntityShared] records
     *        to insert.
     * @throws SQLException if an error occurs during insertion.
     */
    override fun insert(vararg records: SyncControl.EntityShared) {
        transaction {
            records.forEach { record ->
                insertOrThrow(
                    DataSharedTable.TABLE_NAME,
                    DataSharedTable.mapToCreate(
                        record.entityId,
                        record.entityName,
                        record.data.encodeToJSON()
                    )
                )
            }
        }
    }

    /**
     * Removes all records from the shared data table.
     *
     * This method executes a DELETE statement directly on the driver,
     * clearing the table of all entries.
     *
     * @throws SQLException if an error occurs during deletion.
     */
    override fun truncate() {
        driver.handle {
            execute("DELETE FROM ${DataSharedTable.TABLE_NAME};")
        }
    }

    /**
     * Queries records from the shared data table based on the specified
     * [QueryBuilder].
     *
     * The resulting rows are mapped to a list of key-value pairs, where
     * each map corresponds to a single record. The list is returned in
     * reverse insertion order.
     *
     * @param builder A [QueryBuilder] instance configured with the
     *        desired selection and filtering criteria.
     * @return A [List] of [DataMap], each representing a row from the
     *         table as a map of column names to values.
     * @throws SQLException if an error occurs during the query.
     */
    override fun queryRecords(builder: QueryBuilder): List<DataMap> {
        return rawQuery(builder.build()) {
            mapOf(
                DataSharedTable.ATTR_ID to it.getString(0),
                DataSharedTable.ATTR_ENTITY_NAME to it.getString(1),
                DataSharedTable.ATTR_DATA to it.getString(2)
            )
        }
    }

}
