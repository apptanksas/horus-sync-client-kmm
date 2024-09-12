package com.apptank.horus.client.sync.tasks

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_3
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.buildEntitiesSchemeFromJSON
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.di.IDatabaseDriverFactory
import com.apptank.horus.client.di.INetworkValidator
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import com.apptank.horus.client.tasks.ControlTaskManager
import com.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
import com.russhwolf.settings.Settings
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import kotlin.test.fail


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ControlTaskManagerTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver

    @Mock
    val networkValidator: INetworkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val migrationService = mock(classOf<IMigrationService>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    @Mock
    val databaseDriverFactory = mock(classOf<IDatabaseDriverFactory>())

    @Mock
    val storageSettings = mock(classOf<Settings>())


    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val database = HorusDatabase("database.db", driver)

        every { databaseDriverFactory.getDatabaseName() }.returns("database.db")
        every { databaseDriverFactory.getDriver() }.returns(driver)
        every { databaseDriverFactory.getDatabase() }.returns(database)
        every { databaseDriverFactory.getSchema() }.returns(HorusDatabase.Schema)

        with(HorusContainer) {
            setupNetworkValidator(networkValidator)
            setupSettings(storageSettings)
            setupMigrationService(migrationService)
            setupSynchronizationService(synchronizationService)
            setupDatabaseFactory(databaseDriverFactory)
            setupBaseUrl("http://dev.horus.com")
        }

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
    }

    @After
    fun tearDown() {
        HorusAuthentication.clearSession()
    }

    @Test
    fun `start execution complete successfully`() = runBlocking {
        // Given
        val entitiesScheme = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_3)
        val entitiesData = listOf(
            SyncDTO.Response.Entity(
                "farms",
                mapOf(
                    "id" to uuid(),
                    "sync_hash" to randomHash(),
                    "sync_owner_id" to uuid(),
                    "sync_created_at" to timestamp(),
                    "sync_updated_at" to timestamp(),
                    "mv_area_total" to "1",
                    "mv_area_cow_farming" to "1",
                    "measure_milk" to "kg",
                    "measure_weight" to "kg",
                    "type" to "1",
                    "name" to "Farm 1",
                    "destination" to "1",
                )
            )
        )

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

        every { networkValidator.isNetworkAvailable() }.returnsMany(true, true, false)

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
        Assert.assertEquals(
            taskExecutionCountExpected,
            ControlTaskManager.getTaskExecutionCounter()
        )
        Assert.assertTrue(isCompleted)
    }
}