package com.apptank.horus.client.migration.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.migration.network.dto.MigrationDTO

interface IMigrationService {
    suspend fun getMigration(): DataResult<List<MigrationDTO.Response.EntityScheme>>
}