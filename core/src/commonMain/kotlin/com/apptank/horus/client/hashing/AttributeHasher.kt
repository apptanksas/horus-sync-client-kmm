package com.apptank.horus.client.hashing

import com.apptank.horus.client.data.EntityAttribute
import io.ktor.utils.io.core.toByteArray
import org.kotlincrypto.hash.sha2.SHA256


object AttributeHasher {

    fun generateHash(attributes: List<EntityAttribute<*>>): String {
        val inputString = attributes.sortedBy { it.name }
            .joinToString(separator = "", transform = { it.value.toString() })
        return sha256(inputString)
    }

    fun generateHashFromList(data: List<String>): String {
        return generateHash(data.map { EntityAttribute(it, it) })
    }

    private fun sha256(input: String): String {
        val sha256 = SHA256()
        val hashBytes = sha256.digest(input.toByteArray())
        return hashBytes.joinToString("") { it.toHex() }
    }

    private fun Byte.toHex(): String = this.toUByte().toString(16).padStart(2, '0')
}