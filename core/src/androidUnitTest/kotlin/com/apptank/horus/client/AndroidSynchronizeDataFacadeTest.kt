package com.apptank.horus.client

import android.app.Activity
import android.content.Context
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.migration.database.DatabaseSchema
import com.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.migration.network.toScheme
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import com.russhwolf.settings.Settings
import horus.HorusDatabase
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.mock
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AndroidSynchronizeDataFacadeTest : TestCase() {

    private lateinit var databaseFactory: IDatabaseDriverFactory
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var context: Context

    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val migrationService = mock(classOf<IMigrationService>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    @Mock
    val storageSettings = mock(classOf<Settings>())

    @Before
    fun setUp() {
        context = Robolectric.buildActivity(Activity::class.java).get().applicationContext
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        databaseFactory = DatabaseDriverFactory(context, HorusDatabase.Schema)

        with(HorusContainer) {
            setupNetworkValidator(networkValidator)
            setupSettings(storageSettings)
            setupMigrationService(migrationService)
            setupSynchronizationService(synchronizationService)
            setupDatabaseFactory(databaseFactory)
            setupBaseUrl("http://dev.horus.com")
        }
    }

    @Test
    fun `validate method onReady`() {

        var invoked = false
        SynchronizeDataFacade.onReady {
            invoked = true
        }
        EventBus.post(EventType.VALIDATION_COMPLETED)
        assert(invoked)
    }

    @Test
    fun `when is not ready then throw exception`() {
        Assert.assertThrows(IllegalStateException::class.java) {
            SynchronizeDataFacade.insert("table", mapOf("key" to "value"))
        }
    }

    @Test
    fun `validate insert is failure by entity dont exists`() = prepareEnvironment {
        val result = SynchronizeDataFacade.insert("table_not_found", mapOf("key" to "value"))
        assert(result is DataResult.Failure)
    }

    @Test
    fun `validate insert is success`() = prepareEnvironment {
        val result = SynchronizeDataFacade.insert(
            "farms",
            mapOf(
                "mv_area_total" to uuid(),
                "mv_area_cow_farming" to uuid(),
                "measure_milk" to "kg",
                "measure_weight" to "kg",
                "type" to "1"
            )
        )
        assert(result is DataResult.Success)
    }

    private fun prepareEnvironment(block: () -> Unit) = runBlocking {
        SynchronizeDataFacade
        EventBus.post(EventType.VALIDATION_COMPLETED)
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        migrateDatabase()
        block()
    }

    private fun migrateDatabase() {
        val entitiesSchema =
            buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }

        DatabaseSchema(
            databaseFactory.getDatabaseName(), driver, 1, DatabaseTablesCreatorDelegate(
                entitiesSchema
            )
        ).apply {
            create(driver)
            Assert.assertTrue(getTablesNames().contains("farms"))
        }
    }

}