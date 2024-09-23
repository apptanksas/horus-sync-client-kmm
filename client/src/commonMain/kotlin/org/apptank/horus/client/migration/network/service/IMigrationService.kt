package org.apptank.horus.client.migration.network.service

import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.migration.network.dto.MigrationDTO

/**
 * Interface for migration services that provide data related to entity schemes during migration.
 *
 * This interface defines a method for retrieving migration data, specifically a list of entity schemes.
 */
interface IMigrationService {

    /**
     * Retrieves a list of entity schemes related to migration.
     *
     * This method performs a network or database operation to fetch the migration data and returns the result
     * encapsulated in a `DataResult` wrapper. The result contains a list of `MigrationDTO.Response.EntityScheme`
     * objects representing the entity schemes.
     *
     * @return A `DataResult` containing a list of `MigrationDTO.Response.EntityScheme` objects. The result
     *         may indicate success or failure of the operation.
     */
    suspend fun getMigration(): DataResult<List<MigrationDTO.Response.EntityScheme>>
}
