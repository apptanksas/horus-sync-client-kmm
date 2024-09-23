package org.apptank.horus.client.migration.database

import org.apptank.horus.client.database.builder.CreateTableSQLBuilder
import org.apptank.horus.client.migration.domain.EntityScheme
import org.apptank.horus.client.migration.domain.filterRelations

/**
 * This class is responsible for generating and executing SQL statements to create database tables based on a list of entity schemes.
 *
 * @year 2024
 * @author John Ospina
 */
class DatabaseTablesCreatorDelegate(
    private val schemes: List<EntityScheme>
) {

    // List of entities created
    private val entitiesCreated = mutableListOf<EntityScheme>()

    /**
     * Creates tables based on the provided entity schemes and executes the corresponding SQL statements.
     *
     * This method iterates through each entity scheme, generates the necessary SQL statements for table creation, and then executes each SQL statement using the provided `onExecuteSql` function.
     *
     * @param onExecuteSql A lambda function that takes an SQL statement as a parameter and executes it. This function is called for each generated SQL statement.
     */
    fun createTables(onExecuteSql: (sql: String) -> Unit) {
        // Iterate through each scheme and generate SQL sentences to create tables.
        schemes.generateSqlTables().forEach { sql ->
            // Execute SQL statement.
            onExecuteSql(sql)
        }
    }


    /**
     * Gets the list of entities created.
     *
     * @return A list of entity schemes that were created.
     */
    fun getEntitiesCreated(): List<EntityScheme> {
        return entitiesCreated
    }

    /**
     * Generates a list of SQL statements to create tables based on the entity schemes.
     *
     * This method generates SQL statements for the entities related to each scheme and includes the SQL statement to create the table for the current entity scheme. The list of SQL statements can be reversed if required.
     *
     * @param reversed A boolean flag indicating whether to reverse the order of the generated SQL statements. Defaults to `true`.
     * @return A list of SQL statements to create tables.
     */
    private fun List<EntityScheme>.generateSqlTables(reversed: Boolean = true): List<String> {
        val sqlTables = mutableListOf<String>()

        // Iterate through each entity scheme.
        this.forEach { scheme ->
            // Generate SQL statements for related entities and add to the list.
            sqlTables.addAll(scheme.entitiesRelated.generateSqlTables(false))
            // Generate SQL statement to create table for the current entity scheme.
            sqlTables.add(createCreateSQLTable(scheme))
        }

        // Reverse the list of SQL statements if required
        if (reversed)
            return sqlTables.reversed()

        return sqlTables
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

        entitiesCreated.add(scheme)

        return CreateTableSQLBuilder().apply {
            setTableName(scheme.name)
            // Filter attribute types and add them to the table creation statement.
            scheme.attributes.filterRelations().forEach {
                addAttribute(it)
            }
        }.build()
    }
}
