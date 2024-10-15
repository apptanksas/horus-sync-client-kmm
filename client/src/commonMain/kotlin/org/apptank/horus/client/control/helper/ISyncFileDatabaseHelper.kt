package org.apptank.horus.client.control.helper

import org.apptank.horus.client.control.SyncControl

interface ISyncFileDatabaseHelper {

    /**
     * Inserts a file into the database.
     *
     * @param file The file to insert.
     */
    fun insert(file: SyncControl.File)

    /**
     * Updates a file in the database.
     *
     * @param file The file to update.
     * @return `true` if the operation is successful, `false` otherwise.
     */
    fun update(file: SyncControl.File): Boolean

    /**
     * Deletes a file from the database.
     *
     * @param file The file to delete.
     * @return `true` if the operation is successful, `false` otherwise.
     */
    fun delete(file: SyncControl.File): Boolean

    /**
     * Searches for a file in the database.
     *
     * @param reference The reference of the file to search for.
     * @return The file if found, `null` otherwise.
     */
    fun search(reference: String): SyncControl.File?

    /**
     * Searches for a batch of files in the database.
     *
     * @param references The references of the files to search for.
     * @return A list of files if found, an empty list otherwise.
     */
    fun searchBatch(references: List<String>): List<SyncControl.File>

    /**
     * Queries the database for files with a specific status.
     *
     * @param status The status to query by.
     * @return A list of files with the specified status.
     */
    fun queryByStatus(status: SyncControl.FileStatus): List<SyncControl.File>
}