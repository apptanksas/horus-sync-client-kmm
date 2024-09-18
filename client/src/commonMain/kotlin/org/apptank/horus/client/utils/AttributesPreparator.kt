package org.apptank.horus.client.utils

import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.extensions.removeIf
import org.apptank.horus.client.hashing.AttributeHasher

/**
 * An object responsible for preparing and managing attributes related to synchronization.
 */
internal object AttributesPreparator {

    // List of restricted attribute names that should not be included in hashing or insertion.
    private val ATTRIBUTES_RESTRICTED =
        listOf("id", "sync_owner_id", "sync_hash", "sync_created_at", "sync_updated_at")

    /**
     * Checks if any attribute in the provided list contains a restricted name.
     *
     * @param attributes List of [Horus.Attribute] to be checked.
     * @return True if any attribute name is in the restricted list, otherwise false.
     */
    fun isAttributesNameContainsRestricted(attributes: List<Horus.Attribute<*>>): Boolean {
        return attributes.find { ATTRIBUTES_RESTRICTED.contains(it.name.lowercase()) } != null
    }

    /**
     * Appends synchronization-related attributes to the provided list of attributes.
     *
     * @param id The attribute representing the unique identifier.
     * @param attributes List of existing [Horus.Attribute] to which synchronization attributes will be appended.
     * @param userId The user ID to be set as the value for the "sync_owner_id" attribute.
     * @param syncTime The synchronization timestamp. Defaults to the current system time.
     * @return A new list of attributes including the appended synchronization attributes.
     */
    fun appendInsertSyncAttributes(
        id: Horus.Attribute<String>,
        attributes: List<Horus.Attribute<*>>,
        userId: String,
        syncTime: Long = SystemTime.getCurrentTimestamp()
    ): List<Horus.Attribute<*>> {

        return attributes.toMutableList().apply {
            add(id)
            add(Horus.Attribute("sync_owner_id", userId))
            add(Horus.Attribute("sync_created_at", syncTime))
            add(Horus.Attribute("sync_updated_at", syncTime))
        }.toList()
    }

    /**
     * Appends a synchronization hash and updates the timestamp of the attributes.
     *
     * @param id The attribute representing the unique identifier.
     * @param attributes List of existing [Horus.Attribute] to which synchronization attributes will be appended.
     * @param syncTime The synchronization timestamp. Defaults to the current system time.
     * @return A new list of attributes including the appended sync hash and updated timestamp.
     */
    fun appendHashAndUpdateAttributes(
        id: Horus.Attribute<String>,
        attributes: List<Horus.Attribute<*>>,
        syncTime: Long = SystemTime.getCurrentTimestamp()
    ): List<Horus.Attribute<*>> {

        // Generate hash based on attributes for synchronization.
        val hash = Horus.Attribute(
            "sync_hash",
            AttributeHasher.generateHash(
                prepareAttributesForHashing(
                    mutableListOf<Horus.Attribute<*>>(id).apply {
                        addAll(attributes)
                    })
            )
        )

        return attributes.toMutableList().apply {
            add(hash)
            removeIf { it.name == "sync_updated_at" }
            add(Horus.Attribute("sync_updated_at", syncTime))
        }.toList()
    }

    /**
     * Prepares attributes for hashing by filtering out restricted attributes and ensuring uniqueness.
     *
     * @param attributes List of [Horus.Attribute] to be filtered and prepared for hashing.
     * @return A list of attributes filtered from restricted names and distinct by their name.
     */
    private fun prepareAttributesForHashing(attributes: List<Horus.Attribute<*>>): List<Horus.Attribute<*>> {
        val filterAttributes = ATTRIBUTES_RESTRICTED.toMutableList().apply {
            remove("id")
        }
        return attributes.filterNot { filterAttributes.contains(it.name.lowercase()) }
            .distinctBy { it.name }
    }
}
