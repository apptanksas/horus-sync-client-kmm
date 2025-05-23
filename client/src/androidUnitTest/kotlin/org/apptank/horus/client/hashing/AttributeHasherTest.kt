package org.apptank.horus.client.hashing

import org.apptank.horus.client.data.Horus
import org.junit.Assert
import org.junit.Test

class AttributeHasherTest {

    @Test
    fun testHashList() {
        // Given
        val data = listOf<String>("123", "abc")
        // When
        val result = AttributeHasher.generateHashFromList(data)
        // Then
        Assert.assertEquals("dd130a849d7b29e5541b05d2f7f86a4acd4f1ec598c1c9438783f56bc4f0ff80", result)
    }

    @Test
    fun testHashing() {
        // Given
        val attributes = listOf(
            Horus.Attribute("z1", "5a54d7f3-b83b-3193-9bc1-ecff425dee99"),
            Horus.Attribute("age", 76),
            Horus.Attribute("mood", "654711c9-3606-319b-8665-d8c05320833c"),
            Horus.Attribute("date", 1725276225)
        )
        val hashExpected = "4168cbdb0ca6923e633bf61e795c61b0c74908334d71191afb223cad7c91bf64"
        // When
        val result = AttributeHasher.generateHash(attributes)
        // Then
        Assert.assertEquals(hashExpected, result)
    }

    @Test
    fun testHashingWithNullsAndBooleanWithTrue() {
        // Given
        val attributes = listOf(
            Horus.Attribute("id", "43196d90-d8e1-4c24-bbac-76fdfe58a0eb"),
            Horus.Attribute("name", "HHP Animal COW 5539"),
            Horus.Attribute("code", "UUJYWNUMRG"),
            Horus.Attribute("chip_code", "cbfa2352-bff9-4db9-8049-1168e4ee682b596472"),
            Horus.Attribute("gender", "f"),
            Horus.Attribute("type", 1),
            Horus.Attribute("purpose", 2),
            Horus.Attribute("branding_iron_id", null),
            Horus.Attribute("sale_status", 1),
            Horus.Attribute("stage", 4),
            Horus.Attribute("reproductive_status", 1),
            Horus.Attribute("health_status", 1),
            Horus.Attribute("inside", true),
            Horus.Attribute("notes", "5049c394-9445-45c9-9586-49d3690dabea434055"),
            Horus.Attribute("farm_id", "4e1b860c-22dc-477a-a86e-69dde6071874"),
            Horus.Attribute("breed_code", null)
        )
        val hashExpected = "4f707d4007e2ca5cb074f9c1b45b54b79dda8ad75419e50cfcd6a2d661c70b08"
        // When
        val result = AttributeHasher.generateHash(attributes)
        // Then
        Assert.assertEquals(hashExpected, result)
    }

    @Test
    fun testHashingWithNullsAndBooleanWithFalse() {
        // Given
        val attributes = listOf(
            Horus.Attribute("id", "43196d90-d8e1-4c24-bbac-76fdfe58a0eb"),
            Horus.Attribute("name", "HHP Animal COW 5539"),
            Horus.Attribute("code", "UUJYWNUMRG"),
            Horus.Attribute("chip_code", "cbfa2352-bff9-4db9-8049-1168e4ee682b596472"),
            Horus.Attribute("gender", "f"),
            Horus.Attribute("type", 1),
            Horus.Attribute("purpose", 2),
            Horus.Attribute("branding_iron_id", null),
            Horus.Attribute("sale_status", 1),
            Horus.Attribute("stage", 4),
            Horus.Attribute("reproductive_status", 1),
            Horus.Attribute("health_status", 1),
            Horus.Attribute("inside", false),
            Horus.Attribute("notes", "5049c394-9445-45c9-9586-49d3690dabea434055"),
            Horus.Attribute("farm_id", "4e1b860c-22dc-477a-a86e-69dde6071874"),
            Horus.Attribute("breed_code", null)
        )
        val hashExpected = "3ed2ff2ed137d040c1400bc60b175c7ad0af2a5d3bdd2ac9ab2e995de723df00"
        // When
        val result = AttributeHasher.generateHash(attributes)
        // Then
        Assert.assertEquals(hashExpected, result)
    }

    @Test
    fun testHashWithDecimalValues() {
        // Given
        val attributes = listOf(
            Horus.Attribute("id", "5e066af9-e341-45b3-ada5-3ad3e8bc64a7"),
            Horus.Attribute("date", 1747742400),
            Horus.Attribute("animal_type", 1),
            Horus.Attribute("price_total", 3270000.0),
            Horus.Attribute("price_currency", "COP"),
            Horus.Attribute("price_unit", 3000.0f),
            Horus.Attribute("notes", null),
            Horus.Attribute("transfer_status", 1),
            Horus.Attribute("farm_id", "1f3c15ba-9984-4114-bb8f-d3d5ae9ffc03")
        )

        val hashExpected = "e7bbdd37338e8f0f6f4a64459d2183ac0edb2ce39d6e97aeb623e57aa81470ff"
        // When
        val result = AttributeHasher.generateHash(attributes)
        // Then
        Assert.assertEquals(hashExpected, result)
    }

    @Test
    fun testHashWithDecimalValuesCase2() {
        // Given
        val attributes = listOf(
            Horus.Attribute("date", 1747742400),
            Horus.Attribute("animal_type", 1),
            Horus.Attribute("price_total", 32700.05),
            Horus.Attribute("price_currency", "COP"),
            Horus.Attribute("price_unit", 230.233f),
            Horus.Attribute("float", 230.23f),
            Horus.Attribute("float2", 932.20f)
        )

        val hashExpected = "40f1bde7a43cb2d76b4b9ad683b2398f72917e19643ec54a18b0248814c6adb2"

        // When
        val result = AttributeHasher.generateHash(attributes)
        // Then
        Assert.assertEquals(hashExpected, result)
    }


}