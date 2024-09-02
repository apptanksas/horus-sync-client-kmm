package com.apptank.horus.client.hashing

import com.apptank.horus.client.domain.EntityAttribute
import org.junit.Assert
import org.junit.Test

class AttributeHasherTest{

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
    fun testHashing(){
        // Given
        val attributes = listOf(
            EntityAttribute("z1", "5a54d7f3-b83b-3193-9bc1-ecff425dee99"),
            EntityAttribute("age", 76),
            EntityAttribute("mood", "654711c9-3606-319b-8665-d8c05320833c"),
            EntityAttribute("date", 1725276225)
        )
        val hashExpected = "4168cbdb0ca6923e633bf61e795c61b0c74908334d71191afb223cad7c91bf64"
        // When
        val result = AttributeHasher.generateHash(attributes)
        // Then
        Assert.assertEquals(hashExpected, result)
    }


}