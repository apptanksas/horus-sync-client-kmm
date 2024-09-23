package org.apptank.horus.client.migration.network.service

import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.network.BaseService
import org.apptank.horus.client.migration.network.dto.MigrationDTO
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of the `IMigrationService` interface that provides migration data using an HTTP client.
 *
 * This class extends `BaseService` and implements `IMigrationService` to fetch migration-related data from
 * a specified URL. It utilizes an `HttpClientEngine` for making HTTP requests.
 *
 * @param engine The HTTP client engine used to perform network operations.
 * @param baseUrl The base URL for the HTTP requests.
 * @constructor Creates a new instance of `MigrationService` with the specified HTTP client engine and base URL.
 */
internal class MigrationService(
    engine: HttpClientEngine,
    baseUrl: String
) : BaseService(engine, baseUrl), IMigrationService {

    /**
     * Retrieves a list of entity schemes related to migration.
     *
     * This method performs an HTTP GET request to the "migration" endpoint and processes the response
     * to return a `DataResult` containing a list of `MigrationDTO.Response.EntityScheme` objects. The result
     * is serialized from the response using the provided `serialize` function.
     *
     * @return A `DataResult` containing a list of `MigrationDTO.Response.EntityScheme` objects. The result
     *         indicates the success or failure of the operation.
     */
    override suspend fun getMigration(): DataResult<List<MigrationDTO.Response.EntityScheme>> =
        get("migration") { it.serialize() }
}
