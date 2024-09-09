package com.apptank.horus.client.hashing

import com.apptank.horus.client.data.Horus
import io.ktor.utils.io.core.toByteArray
import org.kotlincrypto.hash.sha2.SHA256


/**
 * Object responsible for hashing attributes and lists of strings using SHA-256 algorithm.
 *
 * Provides functionality to generate a hash from a list of `Horus.Attribute` objects or a list of strings.
 */
object AttributeHasher {

    /**
     * Generates a SHA-256 hash from a list of `Horus.Attribute` objects.
     *
     * The attributes are sorted by their name, and their values are concatenated into a single string.
     * This string is then hashed using the SHA-256 algorithm.
     *
     * @param attributes A list of `Horus.Attribute` objects to hash.
     * @return The SHA-256 hash of the concatenated string representation of attribute values.
     */
    fun generateHash(attributes: List<Horus.Attribute<*>>): String {
        val inputString = attributes.sortedBy { it.name }
            .joinToString(separator = "", transform = { it.value.toString() })
        return sha256(inputString)
    }

    /**
     * Generates a SHA-256 hash from a list of strings.
     *
     * Each string in the list is used to create a `Horus.Attribute` object where both the name and value are
     * set to the string itself. These attributes are then hashed using the SHA-256 algorithm.
     *
     * @param data A list of strings to hash.
     * @return The SHA-256 hash of the concatenated string representation of the input list.
     */
    fun generateHashFromList(data: List<String>): String {
        return generateHash(data.map { Horus.Attribute(it, it) })
    }

    /**
     * Computes the SHA-256 hash of a given input string.
     *
     * This method utilizes a SHA-256 digest to convert the input string into a hash represented as a hexadecimal string.
     *
     * @param input The input string to hash.
     * @return The SHA-256 hash of the input string, represented as a hexadecimal string.
     */
    private fun sha256(input: String): String {
        val sha256 = SHA256()
        val hashBytes = sha256.digest(input.toByteArray())
        return hashBytes.joinToString("") { it.toHex() }
    }

    /**
     * Converts a Byte to its hexadecimal string representation.
     *
     * @return The hexadecimal string representation of the Byte value, padded to ensure it is always two characters long.
     */
    private fun Byte.toHex(): String = this.toUByte().toString(16).padStart(2, '0')
}
