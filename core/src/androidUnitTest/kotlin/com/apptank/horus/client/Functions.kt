package com.apptank.horus.client

import com.apptank.horus.client.migration.network.dto.MigrationDTO
import com.apptank.horus.client.sync.network.dto.SyncDTO
import kotlinx.serialization.json.Json

private val decoder = Json { ignoreUnknownKeys = true }

fun buildEntitiesSchemeFromJSON(json: String): List<MigrationDTO.Response.EntityScheme> {
    return decoder.decodeFromString<List<MigrationDTO.Response.EntityScheme>>(json)
}

fun buildEntitiesDataFromJSON(json: String): List<SyncDTO.Response.Entity> {
    return decoder.decodeFromString<List<SyncDTO.Response.Entity>>(json)
}