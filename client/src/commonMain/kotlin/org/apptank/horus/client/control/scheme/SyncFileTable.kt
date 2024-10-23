package org.apptank.horus.client.control.scheme

import org.apptank.horus.client.control.SyncControl

/**
 * Object that defines the schema and utility functions for the `sync_files` table in the database.
 *
 * @author John Ospina
 * @year 2024
 */
internal object SyncFileTable {

    const val TABLE_NAME = "horus_sync_files"

    // Column names
    const val ATTR_TYPE = "type"
    const val ATTR_STATUS = "status"
    const val ATTR_REFERENCE = "reference"
    const val ATTR_URL_LOCAL = "url_local"
    const val ATTR_URL_REMOTE = "url_remote"
    const val ATTR_MIME_TYPE = "mime_type"

    // SQL statement to create the table if it does not exist
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_REFERENCE STRING PRIMARY KEY  NOT NULL," +
                "$ATTR_TYPE INTEGER NOT NULL," +
                "$ATTR_STATUS INTEGER NOT NULL," +
                "$ATTR_URL_LOCAL STRING," +
                "$ATTR_URL_REMOTE STRING," +
                "$ATTR_MIME_TYPE STRING)"


    fun mapToCreate(
        file: SyncControl.File
    ) = mapOf(
        ATTR_REFERENCE to file.reference,
        ATTR_TYPE to file.type.id,
        ATTR_STATUS to file.status.id,
        ATTR_URL_LOCAL to file.urlLocal,
        ATTR_URL_REMOTE to file.urlRemote,
        ATTR_MIME_TYPE to file.mimeType
    )
}