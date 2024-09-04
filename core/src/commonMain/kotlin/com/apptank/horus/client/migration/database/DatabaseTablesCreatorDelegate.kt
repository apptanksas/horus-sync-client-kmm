package com.apptank.horus.client.migration.database

import com.apptank.horus.client.database.builder.CreateTableSQLBuilder
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.filterRelations

/**
 * Class responsible for creating database tables based on provided entity schemes.
 * @param schemes List of EntityScheme objects representing database entity structures.
 */
class DatabaseTablesCreatorDelegate(
    private val schemes: List<EntityScheme>
) {

    /**
     * Create tables in the database based on the provided entity schemes.
     * @param onExecuteSql Callback function to execute SQL statements.
     */
    fun createTables(onExecuteSql: (sql: String) -> Unit) {
        // Iterate through each scheme and generate SQL sentence to create tables.
        schemes.generateSqlTables().forEach { sql ->
            // Execute SQL statement.
            onExecuteSql(sql)
        }
    }

    /**
     * Generate SQL statements to create tables for each entity scheme.
     * @return List of SQL statements to create tables.
     */
    private fun List<EntityScheme>.generateSqlTables(): List<String> {
        val sqlTables = mutableListOf<String>()

        // Iterate through each entity scheme.
        this.forEach { scheme ->
            // Generate SQL statements for related entities and add to the list.
            sqlTables.addAll(scheme.entitiesRelated.generateSqlTables())
            // Generate SQL statement to create table for the current entity scheme.
            sqlTables.add(createCreateSQLTable(scheme))
        }

        return sqlTables
    }

    /**
     * Create SQL statement to create a table based on the given entity scheme.
     * @param scheme The entity scheme for which the table is to be created.
     * @return SQL statement to create the table.
     */
    private fun createCreateSQLTable(scheme: EntityScheme): String {
        // TODO("Implement foreign key constraints to delete oncascade")
        // TODO("Implement enum types")
        return CreateTableSQLBuilder().apply {
            setTableName(scheme.name)
            // Filter attribute types and add them to the table creation statement.
            scheme.attributes.filterRelations().forEach {
                addAttribute(it)
            }
        }.build()
    }
}