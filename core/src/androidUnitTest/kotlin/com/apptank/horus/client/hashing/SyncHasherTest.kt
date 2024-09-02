package com.apptank.horus.client.hashing

import org.junit.Assert
import org.junit.Test


class SyncHasherTest {


    @Test
    fun testHashList() {
        // Given
        val data = listOf<String>("123", "abc")
        val hashExpected = "dd130a849d7b29e5541b05d2f7f86a4acd4f1ec598c1c9438783f56bc4f0ff80"
        // When
        val result = AttributeHasher.generateHashFromList(data)
        // Then
        Assert.assertEquals(hashExpected, result)
    }

}