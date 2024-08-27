package com.apptank.horus.client.migration.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.migration.service.dto.EntitySchemeDTO
import com.apptank.horus.client.network.BaseService
import com.apptank.horus.client.network.IMigrationService
import io.ktor.client.engine.HttpClientEngine
class MigrationService(
    engine: HttpClientEngine,
    private val baseUrl: String
) : BaseService(engine), IMigrationService {

    override suspend fun getMigration(): DataResult<List<EntitySchemeDTO>> =
        get("$baseUrl/migration") { it.serialize() }

}