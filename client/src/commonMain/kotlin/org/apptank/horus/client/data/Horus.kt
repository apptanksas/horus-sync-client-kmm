package org.apptank.horus.client.data

import org.apptank.horus.client.base.DataMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * A sealed class representing various elements related to entities in the Horus system.
 */
sealed class Horus {
    /**
     * Represents a batch of entities to be synchronized with the database.
     *
     * @property actions A list of actions to be performed on the entities.
     */
    sealed class Batch {
        /**
         * Represents a batch of entities to be inserted into the database.
         *
         * @property entity The name of the entity.
         * @property attributes A list of attributes associated with the entity.
         */
        data class Insert(val entity: String, val attributes: List<Attribute<*>>) {
            constructor(entity: String, vararg attributes: Attribute<*>) : this(
                entity,
                attributes.toList()
            )

            constructor(entity: String, attributes: DataMap) : this(
                entity,
                attributes.map { Attribute(it.key, it.value) })

            /**
             * Checks if the entity has an attribute with the given name.
             *
             * @param name The name of the attribute.
             * @return True if the entity has the attribute, false otherwise.
             */
            fun hasAttribute(name: String): Boolean = attributes.any { it.name == name }

            /**
             * Retrieves an attribute by its name.
             *
             * @param name The name of the attribute.
             * @return The attribute with the given name, or null if not found.
             */
            fun <T> getAttribute(name: String): T? =
                attributes.find { it.name == name }?.value as T?
        }

        /**
         * Represents a batch of entities to be updated in the database.
         *
         * @property entity The name of the entity.
         * @property id The ID of the entity.
         * @property attributes A list of attributes associated with the entity.
         */
        data class Update(
            val entity: String,
            val id: String,
            val attributes: List<Horus.Attribute<*>>
        ) {
            constructor(entity: String, id: String, vararg attributes: Attribute<*>) : this(
                entity,
                id,
                attributes.toList()
            )

            constructor(entity: String, id: String, attributes: DataMap) : this(
                entity,
                id,
                attributes.map { Attribute(it.key, it.value) })
        }
    }


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
         * Retrieves an integer attribute nullable value by its name.
         *
         * @param name The name of the attribute.
         * @return The integer value of the attribute.
         */
        fun getInt(name: String): Int? = data[name] as Int?

        /**
         * Retrieves a string attribute nullable value by its name.
         *
         * @param name The name of the attribute.
         * @return The string value of the attribute.
         */
        fun getString(name: String): String? = data[name] as String?

        /**
         * Retrieves a boolean attribute nullable value by its name.
         *
         * @param name The name of the attribute.
         * @return The boolean value of the attribute.
         */
        fun getBoolean(name: String): Boolean? =
            attributes.find { it.name == name }?.value as Boolean?

        /**
         * Retrieves a double attribute nullable value by its name.
         *
         * @param name The name of the attribute.
         * @return The double value of the attribute.
         */
        fun getDouble(name: String): Double? = data[name] as Double?

        /**
         * Retrieves a float attribute nullable value by its name.
         *
         * @param name The name of the attribute.
         * @return The float value of the attribute.
         */
        fun getFloat(name: String): Float? = data[name] as Float?

        /**
         * Retrieves a long attribute nullable value by its name.
         *
         * @param name The name of the attribute.
         * @return The long value of the attribute.
         */
        fun getLong(name: String): Long? = data[name] as Long?

        /**
         * Retrieves an integer attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The integer value of the attribute.
         *
         * @throws NullPointerException if the attribute is not found.
         */
        fun getRequireInt(name: String): Int = data[name] as Int

        /**
         * Retrieves a String attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The UUID value of the attribute.
         *
         * @throws NullPointerException if the attribute is not found.
         */
        fun getRequireString(name: String): String = data[name] as String

        /**
         * Retrieves a boolean attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The boolean value of the attribute.
         *
         * @throws NullPointerException if the attribute is not found.
         */
        fun getRequireBoolean(name: String): Boolean = data[name] as Boolean

        /**
         * Retrieves a double attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The double value of the attribute.
         *
         * @throws NullPointerException if the attribute is not found.
         */
        fun getRequireDouble(name: String): Double = data[name] as Double

        /**
         * Retrieves a float attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The float value of the attribute.
         *
         * @throws NullPointerException if the attribute is not found.
         */
        fun getRequireFloat(name: String): Float = data[name] as Float

        /**
         * Retrieves a long attribute value by its name.
         *
         * @param name The name of the attribute.
         * @return The long value of the attribute.
         *
         * @throws NullPointerException if the attribute is not found.
         */
        fun getRequireLong(name: String): Long = data[name] as Long

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
     *
     * @property id The ID of the file.
     * @property length The length of the ID.
     * @constructor Creates a new file reference object with the given ID and length.
     * @return A new file reference object.
     */
    data class FileReference
    @OptIn(ExperimentalUuidApi::class)
    internal constructor(
        private val id: CharSequence = Uuid.random().toString(),
        override val length: Int = id.length
    ) :
        Comparable<FileReference>, CharSequence {

        override fun compareTo(other: FileReference): Int {
            return id.toString().compareTo(id.toString())
        }

        override fun get(index: Int): Char {
            return id[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return id.subSequence(startIndex, endIndex)
        }

        override fun toString(): String {
            return id.toString()
        }
    }
}
