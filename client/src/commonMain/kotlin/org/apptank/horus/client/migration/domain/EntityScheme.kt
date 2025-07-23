package org.apptank.horus.client.migration.domain

/**
 * Represents an entity scheme that includes information about an entity, its attributes, and related entities.
 *
 * @param name The name of the entity.
 * @param type The type of the entity, which could be either WRITABLE or READABLE.
 * @param attributes A list of attributes associated with the entity.
 * @param currentVersion The current version of the entity scheme.
 * @param entitiesRelated A list of related entity schemes.
 * @param level The level of the entity in a hierarchical structure, default is 0.
 */
data class EntityScheme(
    val name: String,
    val type: EntityType,
    val attributes: List<Attribute>,
    val currentVersion: Long,
    val entitiesRelated: List<EntityScheme>,
    val level: Int = 0 // Used for hierarchical structures
) {
    /**
     * Indicates if the entity is writable.
     *
     * @return `true` if the entity is writable, `false` otherwise.
     */
    fun isWritable() = type == EntityType.WRITABLE
}

/**
 * Enum representing the types of entities.
 */
enum class EntityType {
    WRITABLE,
    READABLE
}

/**
 * Enum representing the types of constraints that can be applied to attributes.
 */
enum class ConstraintType {
    FOREIGN_KEY
}

/**
 * Represents a constraint applied to an attribute, such as a foreign key constraint.
 *
 * @param type The type of the constraint.
 * @param sentence The SQL statement representing the constraint.
 */
data class Constraint(
    val type: ConstraintType,
    val sentence: String
)

/**
 * Represents an attribute of an entity, including its type, nullability, and other properties.
 *
 * @param name The name of the attribute.
 * @param type The type of the attribute.
 * @param isNullable Indicates if the attribute can be null.
 * @param version The version of the attribute.
 * @param options Optional values for attributes of type Enum.
 * @param linkedEntity The name of the linked entity, if applicable (used for relations).
 */
data class Attribute(
    val name: String,
    val type: AttributeType,
    val isNullable: Boolean,
    val version: Long,
    val options: List<String> = listOf(),
    val linkedEntity: String? = null, // Only for relation
    val deleteOnCascade: Boolean = true, // Only for relation
    val regex: String? = null, // Only for custom type attribute
)

/**
 * Enum representing the types of attributes.
 */
enum class AttributeType {
    PrimaryKeyInteger,
    PrimaryKeyString,
    PrimaryKeyUUID,
    Integer,
    Float,
    Boolean,
    String,
    Text,
    Json,
    Enum,
    Custom,
    Timestamp,
    UUID,
    RefFile,
    RelationOneOfMany,
    RelationOneOfOne
}

/**
 * Searches for the last version among a list of entity schemes.
 *
 * This function iterates through each entity scheme and its related entities to find the maximum version number.
 *
 * @return The highest version number found.
 */
fun List<EntityScheme>.getLastVersion(): Long {

    val compareValue = fun(current: Long, new: Long): Long {
        if (new > current) {
            return new
        }
        return current
    }

    var version = 0L

    this.forEach {
        version = compareValue.invoke(version, it.entitiesRelated.getLastVersion())
        it.attributes.forEach {
            version = compareValue.invoke(version, it.version)
        }
    }

    return version
}

/**
 * Filters out attributes of type RelationOneOfMany and RelationOneOfOne from a list of attributes.
 *
 * @return A list of attributes excluding relation types.
 */
fun List<Attribute>.filterRelations(): List<Attribute> {
    return this.filter {
        !listOf(
            AttributeType.RelationOneOfMany,
            AttributeType.RelationOneOfOne
        ).contains(it.type)
    }
}

/**
 * Finds an entity scheme by its name in a list of entity schemes.
 *
 * This function recursively searches through related entities to find the entity with the specified name.
 *
 * @param entityName The name of the entity to search for.
 * @return The `EntityScheme` with the matching name, or `null` if not found.
 */
fun List<EntityScheme>.findByName(entityName: String): EntityScheme? {

    this.forEach {

        it.entitiesRelated.findByName(entityName)?.let {
            return it
        }

        if (it.name == entityName) {
            return it
        }
    }

    return null
}