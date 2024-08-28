package com.apptank.horus.client.migration.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.migration.network.dto.EntitySchemeDTO
import com.apptank.horus.client.base.network.BaseService
import io.ktor.client.engine.HttpClientEngine
class MigrationService(
    engine: HttpClientEngine,
    private val baseUrl: String
) : BaseService(engine), IMigrationService {

    override suspend fun getMigration(): DataResult<List<EntitySchemeDTO>> =
        get("$baseUrl/migration") { it.serialize() }

}