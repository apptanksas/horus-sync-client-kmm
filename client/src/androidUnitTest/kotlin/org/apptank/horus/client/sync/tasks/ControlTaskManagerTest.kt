package org.apptank.horus.client.sync.tasks

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.DATA_MIGRATION_VERSION_3
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.buildEntitiesSchemeFromJSON
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.IDatabaseDriverFactory
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.migration.network.service.IMigrationService
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.apptank.horus.client.tasks.ControlTaskManager
import org.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
import com.russhwolf.settings.Settings
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.every
import io.mockative.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.tasks.RefreshReadableEntitiesTask
import org.apptank.horus.client.tasks.RetrieveDataSharedTask
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
            setupConfig(getHorusConfigTest())
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
                "products",
                mapOf(
                    "id" to uuid(),
                    "sync_hash" to randomHash(),
                    "sync_owner_id" to uuid(),
                    "sync_created_at" to timestamp(),
                    "sync_updated_at" to timestamp(),
                    "mv_size" to "1",
                    "mv_variant" to "1",
                    "volume" to "kg",
                    "weight" to "kg",
                    "type" to "1",
                    "name" to "Product  1",
                    "destination" to "1",
                )
            )
        )

        val taskExecutionCountExpected = 7

        coEvery { migrationService.getMigration() }.returns(DataResult.Success(entitiesScheme))
        every { storageSettings.getLongOrNull(ValidateMigrationLocalDatabaseTask.KEY_SCHEMA_VERSION) }.returns(null)
        every { storageSettings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) }.returns(null)
        every { storageSettings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) }.returns(null)

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

        coEvery { synchronizationService.getDataShared() }.returns(DataResult.Success(entitiesData))

        every { networkValidator.isNetworkAvailable() }.returnsMany(true, true, false, true, true)

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