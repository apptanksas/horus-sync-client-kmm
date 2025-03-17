package org.apptank.horus.client.sync.network.dto

import org.apptank.horus.client.TestCase
import org.junit.Test
import kotlin.test.assertEquals


class ExtensionsKtTest : TestCase() {


    @Test
    fun validateToEntityData() {
        // Given
        val mapData = mapOf(
            "string" to "abc123",
            "int" to 123,
            "double" to 123.45,
            "float" to 123.45f,
            "boolean" to true,
            "null" to null
        )
        val entity = SyncDTO.Response.Entity(
            "entity",
            mapData
        )

        // When
        val result = entity.toEntityData()

        // Then
        assertEquals("entity", result.name)
        assertEquals(mapData.size - 1, result.attributes.size)
        assertEquals(mapData["string"], result.getRequireString("string"))
        assertEquals(mapData["int"], result.getRequireInt("int"))
        assertEquals(mapData["double"], result.getRequireDouble("double"))
        assertEquals(mapData["float"], result.getRequireFloat("float"))
        assertEquals(mapData["boolean"], result.getRequireBoolean("boolean"))
    }

}