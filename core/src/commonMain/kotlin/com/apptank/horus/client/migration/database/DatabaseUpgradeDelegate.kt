package com.apptank.horus.client.migration.database

import com.apptank.horus.client.migration.domain.Attribute
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.filterRelations
import com.apptank.horus.client.migration.domain.getLastVersion
import com.apptank.horus.client.migration.database.builder.AlterTableSQLBuilder

/**
 * Class responsible for handling database upgrades based on provided entity schemes.
 * @param schemes List of EntityScheme objects representing database entity structures.
 */
class DatabaseUpgradeDelegate(
    private val schemes: List<EntityScheme>
) {

    // Map to store attributes for each version of the database schema.
    // Structure: <VersionNumber -> <EntityName -> List Attributes>>
    private val versionMapAttributes = mutableMapOf<Long, Map<String, List<Attribute>>>()

    /**
     * Perform migration from oldVersion to currentVersion.
     * @param oldVersion The old version of the database schema.
     * @param currentVersion The current version of the database schema.
     * @param onExecuteSql Callback function to execute SQL statements.
     */
    fun migrate(oldVersion: Long, currentVersion: Long, onExecuteSql: (sql: String) -> Unit) {
        // Generate map of entity attributes for each version.
        mapSchemes()

        // Migrate attributes for each version incrementally.
        for (version: Long in (oldVersion + 1)..currentVersion) {
            versionMapAttributes[version]?.forEach { (entityName, attrs) ->
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
     * Generate map of entity attributes for each version of the schema.
     */
    private fun mapSchemes() {
        // Find the last version of the schema.
        val lastVersion = schemes.getLastVersion()
        val startVersion = 1

        // Generate map for each version.
        for (version in startVersion..lastVersion) {
            versionMapAttributes[version] = schemes.mapEntityAttributesVersion(version)
        }
    }

    /**
     * Map entity attributes for a specific version of the schema.
     * @param versionSearch The version of the schema to map attributes for.
     * @return Map of entity names to their respective attributes for the specified version.
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

}