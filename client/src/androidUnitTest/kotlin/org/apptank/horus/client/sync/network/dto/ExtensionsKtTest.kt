package org.apptank.horus.client.sync.network.dto

import org.apptank.horus.client.TestCase
import org.apptank.horus.client.extensions.isTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


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

    @Test
    fun validateToEntityWithChildren() {
        // Given
        val mapData = getDataWithChildren()

        // When
        val entity = SyncDTO.Response.Entity(
            "entity",
            mapData
        )

        val result = entity.toEntityData()

        // Then
        assertEquals("entity", result.name)
        assertEquals(mapData.size - 1, result.attributes.size)
        assertTrue(result.relations?.isNotEmpty().isTrue())
        assertEquals(1, result.relations?.size)
    }

    @Test
    fun validateToEntityWithChildrenAndSkip() {
        // Given
        val mapData = getDataWithChildren()

        // When
        val entity = SyncDTO.Response.Entity(
            "entity",
            mapData
        )

        val result = entity.toEntityData(skipChildren = true)

        // Then
        assertEquals("entity", result.name)
        assertEquals(mapData.size - 1, result.attributes.size)
        assertTrue(result.relations.isNullOrEmpty())
    }


    private fun getDataWithChildren(): Map<String, Any?> {
        return mapOf(
            "id" to "5e066af9-e341-45b3-ada5-3ad3e8bc64a7",
            "sync_owner_id" to "224f3ad8-5946-4e1a-b299-76311b3bb5d0",
            "sync_hash" to "ff89e28e84e0b167eb030edcc22e17398fdf908a982f00a859945a44986a8d68",
            "sync_created_at" to 1747783980,
            "sync_updated_at" to 1747783980,
            "farm_id" to "1f3c15ba-9984-4114-bb8f-d3d5ae9ffc03",
            "date" to "2025-05-20 12:00:00",
            "animal_type" to "1",
            "price_total" to 3270000,
            "price_currency" to "COP",
            "price_unit" to 3000,
            "transfer_status" to "1",
            "_animals" to arrayListOf(
                linkedMapOf(
                    "entity" to "animals",
                    "data" to mapOf(
                        "id" to "92301b14-4819-4d91-ba79-52434fa606d8",
                        "sync_owner_id" to "224f3ad8-5946-4e1a-b299-76311b3bb5d0",
                        "sync_hash" to "fc256ca586766675e81e2af4281039b7e1610a45119f3a41853afeeda7e53dbd",
                        "sync_created_at" to 1747783980,
                        "sync_updated_at" to 1747783980,
                        "animal_sale_id" to "5e066af9-e341-45b3-ada5-3ad3e8bc64a7",
                        "animal_id" to "31b1c848-ab79-4947-afb6-de49841671b1",
                        "mv_weight" to "93790eae-9e84-41db-954e-b4ead84ffc88",
                        "price_value" to 570000,
                        "price_currency" to "COP",
                        "animal_stage" to 4
                    )
                )
            )
        )
    }

}