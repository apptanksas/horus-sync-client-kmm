package org.apptank.horus.client.control.helper

import org.apptank.horus.client.control.SyncControl

/**
 * ISyncFileDatabaseHelper provides an interface for managing files in a database.
 *
 * @see SyncControl.File
 */
interface ISyncFileDatabaseHelper {


    /**
     * Inserts a file into the database.
     *
     * @param files The files to insert.
     */
    fun insert(vararg files: SyncControl.File)


    /**
     * Updates a file in the database.
     *
     * @param files The files to update.
     * @return `true` if the operation is successful, `false` otherwise.
     */
    fun update(vararg files: SyncControl.File): Boolean

    /**
     * Deletes a file from the database.
     *
     * @param files The files to delete.
     * @return `true` if the operation is successful, `false` otherwise.
     */
    fun delete(vararg files: SyncControl.File): Boolean

    /**
     * Searches for a file in the database.
     *
     * @param reference The reference of the file to search for.
     * @return The file if found, `null` otherwise.
     */
    fun search(reference: CharSequence): SyncControl.File?

    /**
     * Searches for a batch of files in the database.
     *
     * @param references The references of the files to search for.
     * @return A list of files if found, an empty list otherwise.
     */
    fun searchBatch(references: List<CharSequence>): List<SyncControl.File>

    /**
     * Queries the database for files with a specific status.
     *
     * @param status The status to query by.
     * @return A list of files with the specified status.
     */
    fun queryByStatus(status: SyncControl.FileStatus): List<SyncControl.File>
}