package com.apptank.horus.client.migration.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_1
import com.apptank.horus.client.MOCK_RESPONSE_GET_MIGRATION
import com.apptank.horus.client.buildEntitiesFromJSON
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DatabaseSchemaTest {

    private lateinit var driver: JdbcSqliteDriver

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    }


    @Test
    fun migrationIsSuccess() {
        // Given
        val entities = buildEntitiesFromJSON(DATA_MIGRATION_VERSION_1)
        val databaseSchema =
            DatabaseSchema("databaseName", driver, 1, DatabaseTablesCreatorDelegate(entities))
        val countEntitiesExpected = 7

        // When
        databaseSchema.create(driver)

        // Then
        val tables = databaseSchema.getTablesNames()

        Assert.assertEquals(countEntitiesExpected, tables.size)
    }
}