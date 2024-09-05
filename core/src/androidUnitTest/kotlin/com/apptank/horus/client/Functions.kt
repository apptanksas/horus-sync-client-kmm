package com.apptank.horus.client

import com.apptank.horus.client.migration.network.dto.MigrationDTO
import kotlinx.serialization.json.Json

fun buildEntitiesFromJSON(json: String): List<MigrationDTO.Response.EntityScheme> {
    return Json { ignoreUnknownKeys = true }
        .decodeFromString<List<MigrationDTO.Response.EntityScheme>>(json)
}