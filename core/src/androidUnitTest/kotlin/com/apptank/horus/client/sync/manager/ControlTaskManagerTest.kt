package com.apptank.horus.client.sync.manager

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_1
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.buildEntitiesFromJSON
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.migration.network.service.IMigrationService
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.every
import io.mockative.mock
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

    @Before
    fun setUp() {
        with(HorusContainer) {
            setupMigrationService(migrationService)
            setupDatabaseFactory(databaseDriverFactory)
            setupBaseUrl("http://dev.horus.com")
        }
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        every { databaseDriverFactory.createDriver() }.returns(driver)
    }

    @Test
    fun `test execute`() = runBlocking {
        // Given
        val entitiesScheme = buildEntitiesFromJSON(DATA_MIGRATION_VERSION_1)
        coEvery { migrationService.getMigration() }.returns(DataResult.Success(entitiesScheme))
        every { databaseDriverFactory.getDatabaseName() }.returns("database.db")

        var invokedFlag = false
        ControlTaskManager.setOnCallbackStatusListener {
            if(it == ControlTaskManager.Status.COMPLETED) {
                invokedFlag = true
            }
        }

        // When
        ControlTaskManager.start(Dispatchers.Default)
        delay(1000)

        // Then
        Assert.assertTrue(invokedFlag)
    }
}