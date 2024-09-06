package com.apptank.horus.client.sync.tasks.base

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_1
import com.apptank.horus.client.DATA_MIGRATION_VERSION_3
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.buildEntitiesFromJSON
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.migration.domain.getLastVersion
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.migration.network.toScheme
import com.apptank.horus.client.sync.tasks.RetrieveDatabaseSchemeTask
import com.apptank.horus.client.sync.tasks.TaskResult
import com.apptank.horus.client.sync.tasks.ValidateMigrationLocalDatabaseTask
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
    val migrationService = mock(classOf<IMigrationService>())

    @Mock
    val databaseDriverFactory = mock(classOf<IDatabaseDriverFactory>())

    @Before
    fun setup() {
        settings = MapSettings()
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        every { databaseDriverFactory.createDriver() }.returns(driver)
        every { databaseDriverFactory.getDatabaseName() }.returns("test")

        task = ValidateMigrationLocalDatabaseTask(
            settings,
            databaseDriverFactory,
            RetrieveDatabaseSchemeTask(migrationService)
        )
    }

    @Test
    fun `when don't exists a database then create database scheme`() = runBlocking {
        // Given
        val entitiesScheme = buildEntitiesFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        val countEntitiesExpected = 6

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
                buildEntitiesFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
            val entitiesSchemeV2 =
                buildEntitiesFromJSON(DATA_MIGRATION_VERSION_3).map { it.toScheme() }

            val versionExpected = entitiesSchemeV2.getLastVersion()

            // Create database
            task.execute(entitiesScheme)

            // When
            val result = task.execute(entitiesSchemeV2)

            // Then
            Assert.assertTrue(result is TaskResult.Success)
            Assert.assertEquals(
                versionExpected,
                settings.getLongOrNull(ValidateMigrationLocalDatabaseTask.SCHEMA_VERSION_KEY)
            )
        }

    @Test
    fun `when exists a database and not exists a new version then return success`() = runBlocking {

        // Given

        val entitiesScheme =
            buildEntitiesFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        settings.putLong(
            ValidateMigrationLocalDatabaseTask.SCHEMA_VERSION_KEY,
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
        val result = task.execute(buildEntitiesFromJSON(DATA_MIGRATION_VERSION_1))

        // Then
        Assert.assertTrue(result is TaskResult.Failure)
    }
}