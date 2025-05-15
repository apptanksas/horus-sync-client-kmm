package org.apptank.horus.client.sync.tasks.base

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.DATA_MIGRATION_VERSION_1
import org.apptank.horus.client.DATA_MIGRATION_VERSION_3
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.buildEntitiesSchemeFromJSON
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.di.IDatabaseDriverFactory
import org.apptank.horus.client.migration.domain.getLastVersion
import org.apptank.horus.client.migration.network.toScheme
import org.apptank.horus.client.tasks.TaskResult
import org.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
import com.russhwolf.settings.MapSettings
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.every
import io.mockative.mock
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class ValidateMigrationLocalDatabaseTaskTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var settings: MapSettings
    private lateinit var task: ValidateMigrationLocalDatabaseTask

    @Mock
    val databaseDriverFactory = mock(classOf<IDatabaseDriverFactory>())

    @Before
    fun setup() {
        settings = MapSettings()
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        every { databaseDriverFactory.getDriver() }.returns(driver)
        every { databaseDriverFactory.getDatabaseName() }.returns("test")
        every { databaseDriverFactory.getSchema() }.returns(HorusDatabase.Schema)

        task = ValidateMigrationLocalDatabaseTask(
            settings,
            databaseDriverFactory,
            getMockRetrieveDatabaseSchemeTask(),
        )
    }

    @Test
    fun `when don't exists a database then create database scheme`() = runBlocking {
        // Given
        val entitiesScheme = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        val countEntitiesExpected = 12

        // When
        val result = task.execute(entitiesScheme)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
        val tables = driver.rawQuery(QUERY_TABLES) { it.getString(0) }
        Assert.assertEquals(countEntitiesExpected, tables.size)
    }

    @Test
    fun `when exists a database and exists a new version then migrate to new version`() =
        runBlocking {

            // Given
            val entitiesScheme =
                buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
            val entitiesSchemeV2 =
                buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_3).map { it.toScheme() }

            val versionExpected = entitiesSchemeV2.getLastVersion()

            // Create database
            task.execute(entitiesScheme)

            // When
            val result = task.execute(entitiesSchemeV2)

            // Then
            Assert.assertTrue(result is TaskResult.Success)
            Assert.assertEquals(
                versionExpected,
                settings.getLongOrNull(ValidateMigrationLocalDatabaseTask.KEY_SCHEMA_VERSION)
            )
        }

    @Test
    fun `when exists a database and not exists a new version then return success`() = runBlocking {

        // Given

        val entitiesScheme =
            buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        settings.putLong(
            ValidateMigrationLocalDatabaseTask.KEY_SCHEMA_VERSION,
            entitiesScheme.getLastVersion()
        )

        // Create database
        task.execute(entitiesScheme)

        // When
        val result = task.execute(entitiesScheme)

        // Then
        Assert.assertTrue(result is TaskResult.Success)
    }

    @Test
    fun `when occurred an error then result failure`() = runBlocking {
        // When
        val result = task.execute(buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1))

        // Then
        Assert.assertTrue(result is TaskResult.Failure)
    }
}