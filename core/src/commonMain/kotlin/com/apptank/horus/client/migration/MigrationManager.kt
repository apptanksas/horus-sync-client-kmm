package com.apptank.horus.client.migration

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.migration.database.DatabaseSchema
import com.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.getLastVersion

class MigrationManager(
    private val entities: List<EntityScheme>
) {

    /* private val databaseScheme = DatabaseSchema(
         getLastVersion(),
         DatabaseTablesCreatorDelegate(entities)
     )*/

    fun start(): DataResult<Unit> {
        return DataResult.Success(Unit)
    }

    fun getLastVersion(): Long {
        return entities.getLastVersion()
    }
}