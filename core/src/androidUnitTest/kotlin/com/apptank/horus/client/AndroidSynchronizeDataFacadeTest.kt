package com.apptank.horus.client

import android.app.Activity
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.migration.network.toScheme
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import com.russhwolf.settings.Settings
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.mock
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    private lateinit var context: Context
    private lateinit var driver: SqlDriver

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
        databaseFactory = DatabaseDriverFactory(context)
        driver = databaseFactory.createDriver()

        with(HorusContainer) {
            setupNetworkValidator(networkValidator)
            setupSettings(storageSettings)
            setupMigrationService(migrationService)
            setupSynchronizationService(synchronizationService)
            setupDatabaseFactory(databaseFactory)
            setupBaseUrl("http://dev.horus.com")
        }
    }

    @After
    fun tearDown() {
        driver.close()
        SynchronizeDataFacade.clear()
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
    fun `when is not ready then throw exception because is not ready`(): Unit = runBlocking {
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
                "type" to "1",
                "name" to "Farm 1",
                "destination" to "1"
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
            buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_3).map { it.toScheme() }
        HorusDatabase.Schema.create(driver, entitiesSchema)

        driver.also {
            Assert.assertTrue("table farms not exists", it.getTablesNames().contains("farms"))
        }
    }


}