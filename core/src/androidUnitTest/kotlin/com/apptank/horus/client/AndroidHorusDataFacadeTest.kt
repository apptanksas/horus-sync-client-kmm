package com.apptank.horus.client

import android.app.Activity
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.fold
import com.apptank.horus.client.data.DataChangeListener
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.database.SQL
import com.apptank.horus.client.exception.EntityNotExistsException
import com.apptank.horus.client.exception.EntityNotWritableException
import com.apptank.horus.client.extensions.execute
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
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AndroidHorusDataFacadeTest : TestCase() {

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
        driver = databaseFactory.getDriver()

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
        HorusDataFacade.clear()
        HorusContainer.clear()
        driver.close()
        HorusDataFacade.removeAllDataChangeListeners()
    }

    @Test
    fun `validate method onReady`() {

        var invoked = false
        HorusDataFacade.onReady {
            invoked = true
        }
        EventBus.emit(EventType.VALIDATION_COMPLETED)
        assert(invoked)
    }

    @Test
    fun `when is not ready then throw exception because is not ready`(): Unit = runBlocking {
        Assert.assertThrows(IllegalStateException::class.java) {
            HorusDataFacade.insert("table", mapOf("key" to "value"))
        }
    }


    @Test
    fun `multiples tests associated`() = prepareEnvironment {

        var invokedInsert = false
        var invokedUpdate = false
        var invokedDelete = false

        HorusDataFacade.addDataChangeListener(object : DataChangeListener {

            override fun onInsert(entity: String, id: String, data: DataMap) {
                invokedInsert = true
            }

            override fun onUpdate(entity: String, id: String, data: DataMap) {
                invokedUpdate = true
            }

            override fun onDelete(entity: String, id: String) {
                invokedDelete = true
            }
        })

        validateEntityIsNotWritable()
        validatesOperationIsFailureByEntityNoExists()
        validateInsertTest()
        validateInsertAndUpdateIsSuccess()
        validateInsertAndDeleteIsSuccess()
        validateGetEntityByIdReturnEntity()
        validateGetEntityByIdReturnNull()
        validateGetEntities()
        validateGetEntitiesWithWhereConditions()
        validateGetEntitiesWithLimitAndOffset()

        assert(invokedInsert)
        assert(invokedUpdate)
        assert(invokedDelete)
    }

    private fun validateEntityIsNotWritable() = prepareInternalTest {
        Assert.assertThrows(EntityNotWritableException::class.java) {
            HorusDataFacade.insert("animal_breeds", mapOf("key" to "value"))
        }
    }

    private suspend fun validatesOperationIsFailureByEntityNoExists() = prepareInternalTest {

        val entityName = "any_table_" + Random.nextInt()

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.insert(entityName, mapOf("key" to "value"))
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.updateEntity(entityName, "id", mapOf("key" to "value"))
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.deleteEntity(entityName, "id")
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.getEntityById(entityName, "id")
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            runBlocking {
                HorusDataFacade.getEntities(entityName)
            }
        }

    }

    private fun validateInsertTest() = prepareInternalTest {
        val result = HorusDataFacade.insert(
            "measures_values",
            createDataInsertRecord()
        )
        assert(result is DataResult.Success)
    }

    private fun validateInsertAndUpdateIsSuccess() = prepareInternalTest {
        // Given
        val valueExpected = Random.nextFloat()
        val resultInsert = HorusDataFacade.insert(
            "measures_values",
            createDataInsertRecord()
        )

        // When

        val resultUpdate = if (resultInsert is DataResult.Success) {
            HorusDataFacade.updateEntity(
                "measures_values",
                resultInsert.data,
                mapOf("value" to valueExpected)
            )
        } else {
            DataResult.Failure(Exception("Error"))
        }


        // Then
        assert(resultInsert is DataResult.Success)
        assert(resultUpdate is DataResult.Success)

        if (resultInsert is DataResult.Success) {
            assert(driver.rawQuery("SELECT * FROM measures_values WHERE id= '" + resultInsert.data + "' AND value = $valueExpected") {
                it.getString(0)
            }.isNotEmpty())
        } else {
            fail()
        }
    }

    private fun validateInsertAndDeleteIsSuccess() = prepareInternalTest {
        // Given
        val resultInsert = HorusDataFacade.insert(
            "measures_values",
            createDataInsertRecord()
        )

        // When
        val resultDelete = if (resultInsert is DataResult.Success) {
            HorusDataFacade.deleteEntity("measures_values", resultInsert.data)
        } else {
            DataResult.Failure(Exception("Error"))
        }

        // Then
        assert(resultInsert is DataResult.Success)
        assert(resultDelete is DataResult.Success)

        if (resultInsert is DataResult.Success) {
            assert(driver.rawQuery("SELECT * FROM measures_values WHERE id= '" + resultInsert.data + "'") {
                it.getString(0)
            }.isEmpty())
        } else {
            fail()
        }
    }

    private fun validateGetEntityByIdReturnEntity() = prepareInternalTest {
        // Given
        val resultInsert = HorusDataFacade.insert(
            "measures_values",
            createDataInsertRecord()
        )

        // When
        val entity =
            HorusDataFacade.getEntityById(
                "measures_values",
                (resultInsert as DataResult.Success).data
            )

        // Then
        Assert.assertNotNull(entity)
        Assert.assertTrue((entity?.getFloat("value") ?: 0F) > 0f)
    }

    private suspend fun validateGetEntities() = prepareInternalTest {
        // Given
        val attributesList = generateArray {
            createDataInsertRecord().map { Horus.Attribute(it.key, it.value) }
        }

        attributesList.forEach {
            HorusDataFacade.insert("measures_values", *it.toTypedArray())
        }

        // When
        val result =
            HorusDataFacade.getEntities("measures_values")

        result.fold(
            { entities ->
                Assert.assertTrue(entities.isNotEmpty())
                Assert.assertEquals(attributesList.size, entities.size)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }

    private suspend fun validateGetEntitiesWithWhereConditions() = prepareInternalTest {
        // Given
        val attributesList = createDataInsertRecord().map { Horus.Attribute(it.key, it.value) }

        val insertResult = HorusDataFacade.insert("measures_values", *attributesList.toTypedArray())

        // When

        val entityId = getEntityId(insertResult)
        val result =
            HorusDataFacade.getEntities(
                "measures_values",
                listOf(SQL.WhereCondition(SQL.ColumnValue("id", entityId)))
            )

        result.fold(
            { entities ->
                Assert.assertTrue(entities.isNotEmpty())
                Assert.assertEquals(1, entities.size)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }

    private suspend fun validateGetEntitiesWithLimitAndOffset() = prepareInternalTest {

        val attributesList = List(20) { 0 }.map {
            createDataInsertRecord().map { Horus.Attribute(it.key, it.value) }
        }

        attributesList.forEach {
            HorusDataFacade.insert("measures_values", *it.toTypedArray())
        }

        // When
        val result =
            HorusDataFacade.getEntities(
                "measures_values",
                limit = 10,
                offset = 5
            )

        // Then
        result.fold(
            { entities ->
                Assert.assertTrue(entities.isNotEmpty())
                Assert.assertEquals(10, entities.size)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }

    private fun validateGetEntityByIdReturnNull() {
        // When
        val entity =
            HorusDataFacade.getEntityById("measures_values", uuid())

        // Then
        Assert.assertNull(entity)
    }

    private fun createDataInsertRecord() = mapOf(
        "measure" to "w",
        "unit" to "kg",
        "value" to 10.0f
    )

    private fun prepareEnvironment(block: suspend () -> Unit) = runBlocking {
        HorusDataFacade
        EventBus.emit(EventType.VALIDATION_COMPLETED)
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        migrateDatabase()
        block()
    }

    private fun prepareInternalTest(block: suspend () -> Unit) = runBlocking {
        driver.execute("DELETE FROM measures_values")
        driver.execute("DELETE FROM animal_breeds")

        block()
    }

    private fun migrateDatabase() {
        val entitiesSchema =
            buildEntitiesSchemeFromJSON(DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE).map { it.toScheme() }
        HorusDatabase.Schema.create(driver, entitiesSchema)

        driver.also {
            Assert.assertTrue(
                "table measures_values not exists",
                it.getTablesNames().contains("measures_values")
            )
        }
    }

    private fun getEntityId(insertResult: DataResult<String>): String {
        return (insertResult as DataResult.Success).data
    }


}