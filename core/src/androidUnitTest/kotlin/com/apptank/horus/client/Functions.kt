package com.apptank.horus.client

import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.network.dto.MigrationDTO
import com.apptank.horus.client.migration.network.toScheme
import kotlinx.serialization.json.Json

fun buildEntitiesFromJSON(json: String): List<EntityScheme> {
    return Json { ignoreUnknownKeys = true }
        .decodeFromString<List<MigrationDTO.Response.EntityScheme>>(json).map { it.toScheme() }
}