package org.apptank.horus.client.data

import org.apptank.horus.client.TestCase
import org.junit.Assert
import kotlin.test.Test

class HorusTest : TestCase() {

    @Test
    fun validateGetAttributesFromEntity() {
        // Given
        val fileReferenceId = uuid()
        val entity = Horus.Entity(
            "entity",
            listOf(
                Horus.Attribute<String?>("nullable", null),
                Horus.Attribute("int", 1),
                Horus.Attribute("string", "string123"),
                Horus.Attribute("double", 1.0),
                Horus.Attribute("float", 1.0f),
                Horus.Attribute("boolean", true),
                Horus.Attribute("long", 1L),
                Horus.Attribute("file", fileReferenceId)
            )
        )

        // Then
        Assert.assertNull(entity.getInt("nullable"))
        Assert.assertNull(entity.getString("nullable"))
        Assert.assertNull(entity.getBoolean("nullable"))
        Assert.assertNull(entity.getDouble("nullable"))
        Assert.assertNull(entity.getFloat("nullable"))
        Assert.assertNull(entity.getLong("nullable"))
        Assert.assertNull(entity.getFileReference("nullable"))
        Assert.assertEquals(1, entity.getInt("int"))
        Assert.assertEquals("string123", entity.getString("string"))
        Assert.assertEquals(1.0, entity.getDouble("double"))
        Assert.assertEquals(1.0f, entity.getFloat("float"))
        Assert.assertEquals(true, entity.getBoolean("boolean"))
        Assert.assertEquals(1L, entity.getLong("long"))
        Assert.assertEquals(fileReferenceId, entity.getFileReference("file").toString())
        Assert.assertEquals(1, entity.getRequireInt("int"))
        Assert.assertEquals("string123", entity.getRequireString("string"))
        Assert.assertTrue(1.0 == entity.getRequireDouble("double"))
        Assert.assertEquals(1.0f, entity.getRequireFloat("float"))
        Assert.assertEquals(true, entity.getRequireBoolean("boolean"))
        Assert.assertEquals(1L, entity.getRequireLong("long"))
        Assert.assertEquals(fileReferenceId, entity.getRequireString("file"))

        Assert.assertThrows(NullPointerException::class.java) {
            entity.getRequireInt("nullable")
        }
        Assert.assertThrows(NullPointerException::class.java) {
            entity.getRequireString("nullable")
        }
        Assert.assertThrows(NullPointerException::class.java) {
            entity.getRequireDouble("nullable")
        }
        Assert.assertThrows(NullPointerException::class.java) {
            entity.getRequireFloat("nullable")
        }
        Assert.assertThrows(NullPointerException::class.java) {
            entity.getRequireBoolean("nullable")
        }
        Assert.assertThrows(NullPointerException::class.java) {
            entity.getRequireLong("nullable")
        }
        Assert.assertThrows(NullPointerException::class.java) {
            entity.getRequireString("nullable")
        }
    }

    @Test
    fun validatePolymorphicAttributes() {
        // Given
        val entity = Horus.Entity(
            "entity",
            listOf(
                Horus.Attribute("longAsInt", 10233L),
                Horus.Attribute("doubleASFloat", 20.0)
            )
        )

        // When
        val longAsInt = entity.getInt("longAsInt")
        val doubleAsFloat = entity.getFloat("doubleASFloat")
        val longAsIntRequired = entity.getRequireInt("longAsInt")
        val doubleAsFloatRequired = entity.getRequireFloat("doubleASFloat")

        // Then
        Assert.assertEquals(10233, longAsInt)
        Assert.assertEquals(20f, doubleAsFloat)
        Assert.assertEquals(10233, longAsIntRequired)
        Assert.assertEquals(20f, doubleAsFloatRequired)
    }
}