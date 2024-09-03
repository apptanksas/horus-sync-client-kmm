package com.apptank.horus.client.utils

import com.apptank.horus.client.data.EntityAttribute
import com.apptank.horus.client.extensions.removeIf
import com.apptank.horus.client.hashing.AttributeHasher


internal object AttributesPreparator {

    private val ATTRIBUTES_RESTRICTED =
        listOf("id", "sync_owner_id", "sync_hash", "sync_created_at", "sync_updated_at")

    fun isAttributesNameContainsRestricted(attributes: List<EntityAttribute<*>>): Boolean {
        return attributes.find { ATTRIBUTES_RESTRICTED.contains(it.name.lowercase()) } != null
    }

    fun appendInsertSyncAttributes(
        id: EntityAttribute<String>,
        attributes: List<EntityAttribute<*>>,
        userId: String,
        syncTime: Long = SystemTime.getCurrentTimestamp()
    ): List<EntityAttribute<*>> {

        return attributes.toMutableList().apply {
            add(id)
            add(EntityAttribute("sync_owner_id", userId))
            add(EntityAttribute("sync_created_at", syncTime))
            add(EntityAttribute("sync_updated_at", syncTime))
        }.toList()
    }

    fun appendHashAndUpdateAttributes(
        id: EntityAttribute<String>,
        attributes: List<EntityAttribute<*>>,
        syncTime: Long = SystemTime.getCurrentTimestamp()
    ): List<EntityAttribute<*>> {

        val hash = EntityAttribute(
            "sync_hash",
            AttributeHasher.generateHash(
                prepareAttributesForHashing(
                    mutableListOf<EntityAttribute<*>>(id).apply {
                        addAll(attributes)
                    })
            )
        )

        return attributes.toMutableList().apply {
            add(hash)
            removeIf { it.name == "sync_updated_at" }
            add(EntityAttribute("sync_updated_at", syncTime))
        }.toList()
    }

    private fun prepareAttributesForHashing(attributes: List<EntityAttribute<*>>): List<EntityAttribute<*>> {
        val filterAttributes = ATTRIBUTES_RESTRICTED.toMutableList().apply {
            remove("id")
        }
        return attributes.filterNot { filterAttributes.contains(it.name.lowercase()) }
            .distinctBy { it.name }
    }
}