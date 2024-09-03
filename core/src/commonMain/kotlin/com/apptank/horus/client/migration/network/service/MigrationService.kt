package com.apptank.horus.client.migration.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.network.BaseService
import com.apptank.horus.client.migration.network.dto.MigrationDTO
import io.ktor.client.engine.HttpClientEngine

internal class MigrationService(
    engine: HttpClientEngine,
    baseUrl: String
) : BaseService(engine, baseUrl), IMigrationService {

    override suspend fun getMigration(): DataResult<List<MigrationDTO.Response.EntityScheme>> =
        get("migration") { it.serialize() }

}