package com.apptank.horus.client.migration.database

import com.apptank.horus.client.migration.domain.Attribute
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.filterRelations
import com.apptank.horus.client.migration.domain.getLastVersion
import com.apptank.horus.client.database.builder.AlterTableSQLBuilder
import com.apptank.horus.client.database.builder.CreateTableSQLBuilder
import com.apptank.horus.client.migration.domain.findByName

/**
 * This class handles the process of upgrading a database schema by migrating changes from an old version to a current version.
 * It generates and executes the necessary SQL statements to create new tables and update existing ones according to the provided entity schemes.
 *
 * @year 2024
 * @author John Ospina
 */
class DatabaseUpgradeDelegate(
    private val schemes: List<EntityScheme>
) {

    // Map to store attributes for each version of the database schema.
    // Structure: <VersionNumber -> <EntityName -> List Attributes>>
    private val versionMapAttributes = mutableMapOf<Long, Map<String, List<Attribute>>>()

    // Map to store entities for each version of the database schema.
    // Structure: <VersionNumber -> List Entities>
    private val versionMapEntities = mutableMapOf<Long, List<String>>()

    private val newEntitiesCreated = mutableListOf<EntityScheme>()

    /**
     * Migrates the database schema from an old version to the current version by generating and executing SQL statements.
     *
     * This method performs incremental migrations for each version between the old version and the current version. It creates new tables and adds new attributes as needed.
     *
     * @param oldVersion The version of the database schema to migrate from.
     * @param currentVersion The version of the database schema to migrate to.
     * @param onExecuteSql A lambda function that takes an SQL statement as a parameter and executes it. This function is called for each generated SQL statement.
     */
    fun migrate(oldVersion: Long, currentVersion: Long, onExecuteSql: (sql: String) -> Unit) {
        // Generate map of entity attributes for each version.
        mapSchemes()

        // Migrate attributes for each version incrementally.
        for (version: Long in (oldVersion + 1)..currentVersion) {

            val newTablesAdded = mutableListOf<String>()

            // Create new tables (if applicable).
            versionMapEntities[version]?.forEach {
                schemes.findByName(it)?.let {
                    onExecuteSql(createCreateSQLTable(it))
                    newTablesAdded.add(it.name)
                    newEntitiesCreated.add(it)
                }
            }

            // Create new attributes.
            versionMapAttributes[version]?.forEach { (entityName, attrs) ->

                // Validate if it's a new entity; attributes should not be added to new entities.
                if (newTablesAdded.contains(entityName)) {
                    return@forEach
                }

                attrs.forEach { attribute ->
                    val sqlSentence =
                        AlterTableSQLBuilder().setTableName(entityName).setAttribute(attribute)
                            .build()
                    // Execute SQL statement.
                    onExecuteSql(sqlSentence)
                }
            }
        }
    }

    /**
     * Gets the list of entities created.
     *
     * @return A list of entity schemes that were created.
     */
    fun getNewEntitiesCreated(): List<EntityScheme> {
        return newEntitiesCreated
    }

    /**
     * Maps the schemes to generate a versioned map of attributes and entities.
     *
     * This method creates maps for each version, where each map contains entity names and their associated attributes or a list of entities.
     */
    private fun mapSchemes() {
        // Find the last version of the schema.
        val lastVersion = schemes.getLastVersion()
        val startVersion = 1

        // Generate map for each version.
        for (version in startVersion..lastVersion) {
            versionMapAttributes[version] = schemes.mapEntityAttributesVersion(version)
            versionMapEntities[version] = schemes.mapEntitiesByVersion(version)
        }
    }

    /**
     * Maps the attributes of entities for a specific version.
     *
     * This method recursively maps attributes for entities and their related entities, filtering attributes based on the specified version.
     *
     * @param versionSearch The version of the schema to search for.
     * @return A map of entity names to their attributes for the specified version.
     */
    private fun List<EntityScheme>.mapEntityAttributesVersion(versionSearch: Long): Map<String, List<Attribute>> {
        val mapEntityAttributes = mutableMapOf<String, List<Attribute>>()

        // Iterate through each entity scheme.
        this.forEach {
            // Map attributes for related entities recursively.
            it.entitiesRelated.mapEntityAttributesVersion(versionSearch)
                .forEach { (entityName, attrs) ->
                    mapEntityAttributes[entityName] = attrs
                }

            val entityName: String = it.name
            val attributes = mutableListOf<Attribute>()

            // Filter attributes based on version and add to the map.
            it.attributes.filterRelations().forEach {
                if (it.version == versionSearch) {
                    attributes.add(it)
                }
            }

            mapEntityAttributes[entityName] = attributes
        }

        return mapEntityAttributes
    }

    /**
     * Maps the entities for a specific version.
     *
     * This method recursively maps entities and their related entities, adding entities to the list if they match the specified version.
     *
     * @param versionSearch The version of the schema to search for.
     * @return A list of entity names for the specified version.
     */
    private fun List<EntityScheme>.mapEntitiesByVersion(versionSearch: Long): List<String> {
        val entities = mutableListOf<String>()

        this.forEach {
            // Map entities for related entities recursively.
            it.entitiesRelated.mapEntitiesByVersion(versionSearch).forEach { entityName ->
                entities.add(entityName)
            }

            var entryVersion = Int.MAX_VALUE.toLong()

            it.attributes.forEach {
                if (entryVersion > it.version) {
                    entryVersion = it.version
                }
            }

            if (entryVersion == versionSearch) {
                entities.add(it.name)
            }
        }

        return entities
    }

    /**
     * Creates an SQL statement to create a table based on the provided entity scheme.
     *
     * This method uses a `CreateTableSQLBuilder` to construct the SQL statement for creating a table. It sets the table name and adds attributes to the table creation statement.
     *
     * @param scheme The entity scheme that defines the table to be created.
     * @return The SQL statement to create the table.
     */
    private fun createCreateSQLTable(scheme: EntityScheme): String {
        return CreateTableSQLBuilder().apply {
            setTableName(scheme.name)
            // Filter attribute types and add them to the table creation statement.
            scheme.attributes.filterRelations().forEach {
                addAttribute(it)
            }
        }.build()
    }


}
