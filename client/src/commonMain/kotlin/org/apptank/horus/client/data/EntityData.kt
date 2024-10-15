package org.apptank.horus.client.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * A sealed class representing various elements related to entities in the Horus system.
 */
sealed class Horus {

    /**
     * Represents an entity with a name, attributes, and optional relations to other entities.
     *
     * @property name The name of the entity.
     * @property attributes A list of attributes associated with the entity.
     * @property relations An optional map of relations to other entities, where the key is the relation name and the value is a list of related entities.
     */
    data class Entity(
        val name: String,
        val attributes: List<Attribute<*>>,
        val relations: Map<String, List<Entity>>? = null
    ) {

        /** A map to store attribute values for quick access. */
        private val data = mutableMapOf<String, Any?>()

        init {
            // Initialize the data map with the entity's attributes.
            attributes.forEach {
                data[it.name] = it.value
            }
        }

        /**
         * Retrieves an integer attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The integer value of the attribute.
         */
        fun getInt(name: String): Int = data[name] as Int

        /**
         * Retrieves a string attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The string value of the attribute.
         */
        fun getString(name: String): String = data[name] as String

        /**
         * Retrieves a boolean attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The boolean value of the attribute.
         */
        fun getBoolean(name: String): Boolean =
            attributes.find { it.name == name }?.value as Boolean

        /**
         * Retrieves a double attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The double value of the attribute.
         */
        fun getDouble(name: String): Double = data[name] as Double

        /**
         * Retrieves a float attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The float value of the attribute.
         */
        fun getFloat(name: String): Float = (data[name] as Double).toFloat()

        /**
         * Retrieves a long attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The long value of the attribute.
         */
        fun getLong(name: String): Long = data[name] as Long
    }

    /**
     * Represents an attribute of an entity.
     *
     * @property name The name of the attribute.
     * @property value The value of the attribute.
     */
    data class Attribute<T>(
        val name: String,
        val value: T
    ) {
        internal companion object {
            /** Constant for the attribute name representing the ID. */
            const val ID = "id"

            /** Constant for the attribute name representing the sync hash. */
            const val HASH = "sync_hash"
        }
    }

    /**
     * Represents a hash associated with an entity.
     *
     * @property entity The name of the entity.
     * @property hash The hash value associated with the entity.
     */
    data class EntityHash(
        val entity: String,
        val hash: String
    )

    /**
     * Represents a file reference object containing the file ID.
     */
    data class FileReference
    @OptIn(ExperimentalUuidApi::class)
    internal constructor(private val id: String = Uuid.random().toString()) {
        override fun toString(): String {
            return id
        }
    }
}
