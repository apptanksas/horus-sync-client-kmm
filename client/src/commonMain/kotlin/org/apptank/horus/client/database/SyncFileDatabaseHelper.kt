package org.apptank.horus.client.database

import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper
import org.apptank.horus.client.control.scheme.SyncFileTable
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.Cursor
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.extensions.handle

/**
 * Helper class for performing database operations about table sync file,
 * extending [SQLiteHelper] and implementing [ISyncFileDatabaseHelper].
 *
 * @param databaseName The name of the database.
 * @param driver The SQL driver used to interact with the database.
 */
class SyncFileDatabaseHelper(
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName), ISyncFileDatabaseHelper {


    /**
     * Inserts a file into the database.
     *
     * @param files The files to insert.
     */
    override fun insert(vararg files: SyncControl.File) {
        transaction {
            files.forEach { file ->
                insertOrThrow(
                    SyncFileTable.TABLE_NAME,
                    SyncFileTable.mapToCreate(file)
                )
            }
        }
    }

    /**
     * Updates a file in the database.
     *
     * @param files The files to update.
     * @return `true` if the operation is successful, `false` otherwise.
     */
    override fun update(vararg files: SyncControl.File): Boolean {
        driver.handle {
            runCatching {
                transaction {
                    files.forEach { file ->
                        val whereClause = buildWhereEvaluation(
                            listOf(
                                SQL.WhereCondition(
                                    SQL.ColumnValue(
                                        SyncFileTable.ATTR_REFERENCE,
                                        file.reference
                                    )
                                )
                            )
                        )
                        val result = isOperationIsSuccessful(
                            update(
                                SyncFileTable.TABLE_NAME,
                                SyncFileTable.mapToCreate(file),
                                whereClause
                            )
                        )
                        if (!result) throw IllegalStateException("Update operation failed")
                    }
                }
            }.getOrElse {
                return false
            }
        }
        return true
    }

    /**
     * Deletes files from the database.
     *
     * @param files The files to delete.
     * @return `true` if the operation is successful, `false` otherwise.
     */
    override fun delete(vararg files: SyncControl.File): Boolean {
        driver.handle {
            runCatching {
                transaction {
                    files.forEach { file ->
                        val whereClause = buildWhereEvaluation(
                            listOf(
                                SQL.WhereCondition(
                                    SQL.ColumnValue(
                                        SyncFileTable.ATTR_REFERENCE,
                                        file.reference
                                    )
                                )
                            )
                        )
                        val result =isOperationIsSuccessful(
                            delete(
                                SyncFileTable.TABLE_NAME,
                                whereClause
                            )
                        )
                        if (!result) throw IllegalStateException("Delete operation failed")
                    }
                }
            }.getOrElse {
                return false
            }
        }
        return true
    }

    /**
     * Searches for a file in the database.
     *
     * @param reference The reference of the file to search for.
     * @return The file if found, `null` otherwise.
     */
    override fun search(reference: String): SyncControl.File? {

        val query = SimpleQueryBuilder(SyncFileTable.TABLE_NAME)
            .where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        SyncFileTable.ATTR_REFERENCE,
                        reference
                    )
                )
            )

        val result = queryResult(query.build()) { it.parseToFile() }

        return result.firstOrNull()
    }

    /**
     * Searches for a batch of files in the database.
     *
     * @param references The references of the files to search for.
     * @return A list of files if found, an empty list otherwise.
     */
    override fun searchBatch(references: List<String>): List<SyncControl.File> {
        val query = SimpleQueryBuilder(SyncFileTable.TABLE_NAME).whereIn(
            SyncFileTable.ATTR_REFERENCE,
            references
        )
        return queryResult(query.build()) { it.parseToFile() }
    }

    /**
     * Queries the database for files with a specific status.
     *
     * @param status The status to query by.
     * @return A list of files with the specified status.
     */
    override fun queryByStatus(status: SyncControl.FileStatus): List<SyncControl.File> {
        val query = SimpleQueryBuilder(SyncFileTable.TABLE_NAME)
            .where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        SyncFileTable.ATTR_STATUS,
                        status.id
                    )
                )
            )

        return queryResult(query.build()) { it.parseToFile() }
    }

    /**
     * Parses a [Cursor] to a [SyncControl.File] object.
     *
     * @return The parsed [SyncControl.File] object.
     */
    private fun Cursor.parseToFile(): SyncControl.File {
        return SyncControl.File(
            getValue(SyncFileTable.ATTR_REFERENCE),
            SyncControl.FileType.fromId(getValue(SyncFileTable.ATTR_TYPE)),
            SyncControl.FileStatus.fromId(getValue(SyncFileTable.ATTR_STATUS)),
            getValue(SyncFileTable.ATTR_MIME_TYPE),
            getValueOrNull(SyncFileTable.ATTR_URL_LOCAL),
            getValueOrNull(SyncFileTable.ATTR_URL_REMOTE)
        )
    }

}