package com.apptank.horus.client.migration.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.migration.network.dto.EntitySchemeDTO

interface IMigrationService {
    suspend fun getMigration(): DataResult<List<EntitySchemeDTO>>
}