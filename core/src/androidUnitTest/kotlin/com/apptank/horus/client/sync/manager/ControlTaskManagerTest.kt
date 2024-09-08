package com.apptank.horus.client.sync.manager

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_1
import com.apptank.horus.client.MOCK_RESPONSE_GET_DATA
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.buildEntitiesDataFromJSON
import com.apptank.horus.client.buildEntitiesSchemeFromJSON
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import com.apptank.horus.client.tasks.ControlTaskManager
import com.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
import com.russhwolf.settings.Settings
import horus.HorusDatabase
import io.mockative.Mock
import io.mockative.any
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
import kotlin.test.fail


class ControlTaskManagerTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver

    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val migrationService = mock(classOf<IMigrationService>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

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
            setupNetworkValidator(networkValidator)
            setupSettings(storageSettings)
            setupMigrationService(migrationService)
            setupSynchronizationService(synchronizationService)
            setupDatabaseFactory(databaseDriverFactory)
            setupBaseUrl("http://dev.horus.com")
        }
    }

    @Test
    fun `start execution complete successfully`() = runBlocking {
        // Given
        val entitiesScheme = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1)
        val entitiesData = buildEntitiesDataFromJSON(MOCK_RESPONSE_GET_DATA)

        val taskExecutionCountExpected = 5

        coEvery { migrationService.getMigration() }.returns(DataResult.Success(entitiesScheme))
        every { storageSettings.getLongOrNull(ValidateMigrationLocalDatabaseTask.SCHEMA_VERSION_KEY) }.returns(
            null
        )
        coEvery { synchronizationService.postValidateHashing(any()) }.returns(
            DataResult.Success(
                SyncDTO.Response.HashingValidation(
                    randomHash(),
                    randomHash(),
                    true
                )
            )
        )
        coEvery {
            synchronizationService.getData(any())
        }.returns(DataResult.Success(entitiesData))

        every { networkValidator.isNetworkAvailable() }.returns(false)

        var isCompleted = false

        with(ControlTaskManager) {
            setOnCompleted {
                isCompleted = true
            }
            setOnCallbackStatusListener {
                if (it === ControlTaskManager.Status.FAILED) {
                    fail()
                }
            }
        }

        // When
        ControlTaskManager.start(Dispatchers.Default)
        delay(1000)

        // Then
        Assert.assertTrue(isCompleted)
        Assert.assertEquals(
            taskExecutionCountExpected,
            ControlTaskManager.getTaskExecutionCounter()
        )
    }
}