package org.apptank.horus.client.database

import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper

class SyncFileDatabaseHelper(
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName), ISyncFileDatabaseHelper {


}