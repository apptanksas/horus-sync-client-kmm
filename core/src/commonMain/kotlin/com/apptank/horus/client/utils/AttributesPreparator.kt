package com.apptank.horus.client.utils

import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.extensions.removeIf
import com.apptank.horus.client.hashing.AttributeHasher


internal object AttributesPreparator {

    private val ATTRIBUTES_RESTRICTED =
        listOf("id", "sync_owner_id", "sync_hash", "sync_created_at", "sync_updated_at")

    fun isAttributesNameContainsRestricted(attributes: List<Horus.Attribute<*>>): Boolean {
        return attributes.find { ATTRIBUTES_RESTRICTED.contains(it.name.lowercase()) } != null
    }

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

    fun appendHashAndUpdateAttributes(
        id: Horus.Attribute<String>,
        attributes: List<Horus.Attribute<*>>,
        syncTime: Long = SystemTime.getCurrentTimestamp()
    ): List<Horus.Attribute<*>> {

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

    private fun prepareAttributesForHashing(attributes: List<Horus.Attribute<*>>): List<Horus.Attribute<*>> {
        val filterAttributes = ATTRIBUTES_RESTRICTED.toMutableList().apply {
            remove("id")
        }
        return attributes.filterNot { filterAttributes.contains(it.name.lowercase()) }
            .distinctBy { it.name }
    }
}