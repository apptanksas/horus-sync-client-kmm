package org.apptank.horus.client.migration.network

import org.apptank.horus.client.MOCK_RESPONSE_GET_MIGRATION
import org.apptank.horus.client.migration.domain.getLastVersion
import org.apptank.horus.client.migration.network.dto.MigrationDTO
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test


class MapperKtTest {

    @Test
    fun mapEntitiesFromDTOToDomain() {
        // Given
        val decoder = Json { ignoreUnknownKeys = true }
        val entities = decoder.decodeFromString<List<MigrationDTO.Response.EntityScheme>>(MOCK_RESPONSE_GET_MIGRATION)
        // When
        val entitiesScheme = entities.map { it.toScheme() }
        // Then
        assert(entitiesScheme.isNotEmpty())
        assert(entitiesScheme[1].entitiesRelated.isNotEmpty())
        Assert.assertEquals(1, entitiesScheme.getLastVersion())
    }
}