package com.apptank.horus.client.network

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.migration.service.dto.EntitySchemeDTO

interface IMigrationService {
    suspend fun getMigration(): DataResult<List<EntitySchemeDTO>>
}