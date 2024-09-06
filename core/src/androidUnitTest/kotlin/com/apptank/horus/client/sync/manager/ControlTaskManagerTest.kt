package com.apptank.horus.client.sync.manager

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_1
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.buildEntitiesSchemeFromJSON
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.migration.domain.getLastVersion
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.migration.network.toScheme
import com.apptank.horus.client.tasks.ControlTaskManager
import com.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
import com.russhwolf.settings.Settings
import horus.HorusDatabase
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class ControlTaskManagerTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver

    @Mock
    val migrationService = mock(classOf<IMigrationService>())

    @Mock
    val databaseDriverFactory = mock(classOf<IDatabaseDriverFactory>())

    @Mock
    val storageSettings = mock(classOf<Settings>())

    @Mock
    val horusDatabase = mock(classOf<HorusDatabase>())

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        every { databaseDriverFactory.getDatabaseName() }.returns("database.db")
        every { databaseDriverFactory.createDriver() }.returns(driver)
        every { databaseDriverFactory.retrieveDatabase() }.returns(horusDatabase)

        with(HorusContainer) {
            setupSettings(storageSettings)
            setupMigrationService(migrationService)
            setupDatabaseFactory(databaseDriverFactory)
            setupBaseUrl("http://dev.horus.com")
        }
    }

    @Test
    fun `test execute`() = runBlocking {
        // Given
        val entitiesScheme = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1)
        val schemaVersion = entitiesScheme.map { it.toScheme() }.getLastVersion()

        coEvery { migrationService.getMigration() }.returns(DataResult.Success(entitiesScheme))
        every { storageSettings.getLongOrNull(ValidateMigrationLocalDatabaseTask.SCHEMA_VERSION_KEY) }.returns(
            null
        )

        var invokedFlag = false
        ControlTaskManager.setOnCallbackStatusListener {
            if (it == ControlTaskManager.Status.COMPLETED) {
                invokedFlag = true
            }
        }

        // When
        ControlTaskManager.start(Dispatchers.Default)
        delay(1000)

        // Then
        Assert.assertTrue(invokedFlag)
        verify {
            storageSettings.putLong(
                ValidateMigrationLocalDatabaseTask.SCHEMA_VERSION_KEY,
                schemaVersion
            )
        }.wasInvoked(1)
    }
}