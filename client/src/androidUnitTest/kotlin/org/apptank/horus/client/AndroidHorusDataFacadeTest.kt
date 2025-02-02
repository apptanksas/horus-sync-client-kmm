package org.apptank.horus.client

import android.app.Activity
import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.fold
import org.apptank.horus.client.data.DataChangeListener
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.di.IDatabaseDriverFactory
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.exception.EntityNotExistsException
import org.apptank.horus.client.exception.EntityNotWritableException
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.migration.network.service.IMigrationService
import org.apptank.horus.client.migration.network.toScheme
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import com.russhwolf.settings.Settings
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.coFold
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.extensions.getRequireInt
import org.apptank.horus.client.restrictions.MaxCountEntityRestriction
import org.apptank.horus.client.sync.manager.ISyncFileUploadedManager
import org.apptank.horus.client.sync.manager.RemoteSynchronizatorManager
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
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
import kotlin.random.nextInt
import kotlin.random.nextUInt
import kotlin.test.assertEquals
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
    val uploadFileRepository = mock(classOf<IUploadFileRepository>())

    @Mock
    val storageSettings = mock(classOf<Settings>())

    @Mock
    val fileUploadManager = mock(classOf<ISyncFileUploadedManager>())

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
            setupConfig(getHorusConfigTest())
            setupUploadFileRepository(uploadFileRepository)
            setupSyncFileUploadedManager(fileUploadManager)
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
        EventBus.emit(EventType.ON_READY)
        assert(invoked)
    }

    @Test
    fun `validate method onReady when already is ready`() {
        var invoked = false
        HorusDataFacade.init()
        EventBus.emit(EventType.ON_READY)
        HorusDataFacade.onReady {
            invoked = true
        }
        assert(invoked)
    }

    @Test
    fun `validate method onReady when already user token is setup`() {
        var invoked = false
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        EventBus.emit(EventType.ON_READY)
        HorusDataFacade.onReady {
            invoked = true
        }
        assert(invoked)
    }

    @Test
    fun `validate clear database when user session is cleared`()  {

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        HorusDataFacade.init()
        EventBus.emit(EventType.ON_READY)

        HorusDataFacade.onReady {

            HorusAuthentication.clearSession()

            val countTables = driver.executeQuery(
                null,
                "SELECT * FROM sqlite_master", {
                    if (it.next().value) {
                        return@executeQuery QueryResult.Value(it.getRequireInt(0))
                    }
                    QueryResult.Value(0)
                },
                0
            ).value

            assert(countTables == 0)
        }

        verify { storageSettings.clear() }.wasInvoked()
    }

    @Test
    fun `when is not ready then throw exception because is not ready`(): Unit = runBlocking {
        Assert.assertThrows(IllegalStateException::class.java) {
            HorusDataFacade.insert("table", mapOf("key" to "value"))
        }
    }

    @Test
    fun `when hasDataToSync return true`(): Unit = runBlocking {
        // Given
        val mockSyncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())
        val mockUploadFileRepository = mock(classOf<IUploadFileRepository>())

        with(HorusContainer) {
            setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
            setupUploadFileRepository(mockUploadFileRepository)
        }

        every {
            mockSyncControlDatabaseHelper.getPendingActions()
        }.returns(
            listOf(
                SyncControl.Action(
                    Random.nextInt(), SyncControl.ActionType.INSERT,
                    "entity",
                    SyncControl.ActionStatus.PENDING,
                    emptyMap(), Clock.System.now()
                        .toLocalDateTime(
                            TimeZone.UTC
                        )
                )
            )
        )
        every { mockUploadFileRepository.hasFilesToUpload() }.returns(false)

        // When
        val result = HorusDataFacade.hasDataToSync()
        // Then
        assert(result)
    }

    @Test
    fun `when hasDataToSync return false`(): Unit = runBlocking {
        // Given
        val mockSyncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())
        val mockUploadFileRepository = mock(classOf<IUploadFileRepository>())

        with(HorusContainer) {
            setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
            setupUploadFileRepository(mockUploadFileRepository)
        }

        every {
            mockSyncControlDatabaseHelper.getPendingActions()
        }.returns(emptyList())

        every { mockUploadFileRepository.hasFilesToUpload() }.returns(false)

        // When
        val result = HorusDataFacade.hasDataToSync()
        // Then
        Assert.assertFalse(result)
    }

    @Test
    fun `when getLastSyncDate return null`(): Unit = runBlocking {
        // Given
        val mockSyncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())
        val timestampExpected = Clock.System.now().epochSeconds

        with(HorusContainer) {
            setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
        }

        every {
            mockSyncControlDatabaseHelper.getLastDatetimeCheckpoint()
        }.returns(timestampExpected)

        // When
        val result = HorusDataFacade.getLastSyncDate()
        // Then
        Assert.assertEquals(timestampExpected, result)
    }

    @Test
    fun `when forceSync is invoked and network is not available then invoke onFailure`() =
        runBlocking {

            // Given
            var invoked = false
            val mockNetworkValidator = mock(classOf<INetworkValidator>())
            every { mockNetworkValidator.isNetworkAvailable() }.returns(false)
            HorusContainer.setupNetworkValidator(mockNetworkValidator)

            // When
            HorusDataFacade.forceSync(onFailure = {
                invoked = true
            })

            // Then
            delay(500)
            verify { mockNetworkValidator.isNetworkAvailable() }
            assert(invoked)
        }

    @Test
    fun `when forceSync is invoked and network is available then invoke onSuccess`() =
        runBlocking {
            // Given
            var invokedOnSuccess = false
            var invokedOnFailure = false

            val mockNetworkValidator = mock(classOf<INetworkValidator>())
            val mockMigrationService = mock(classOf<IMigrationService>())
            val mockSyncService = mock(classOf<ISynchronizationService>())
            val mockSettings = mock(classOf<Settings>())
            val mockSyncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())
            val mockOperationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())
            val mockSyncUploadFileManager = mock(classOf<ISyncFileUploadedManager>())
            val mockUploadFileRepository = mock(classOf<IUploadFileRepository>())

            HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
            HorusContainer.setupLogger(KotlinLogger())
            HorusContainer
            every { mockNetworkValidator.isNetworkAvailable() }.returns(true)
            coEvery { mockMigrationService.getMigration() }.returns(
                DataResult.Success(
                    buildEntitiesSchemeFromJSON(DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE)
                )
            )
            every {
                mockSettings.getLongOrNull(ValidateMigrationLocalDatabaseTask.SCHEMA_VERSION_KEY)
            }.returns(1)

            every {
                mockSyncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION)
            }.returns(true)

            every {
                mockSyncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
            }.returns(true)

            every {
                mockSyncControlDatabaseHelper.getPendingActions()
            }.returns(
                listOf(
                    SyncControl.Action(
                        Random.nextInt(), SyncControl.ActionType.INSERT,
                        "entity",
                        SyncControl.ActionStatus.PENDING,
                        emptyMap(), Clock.System.now()
                            .toLocalDateTime(
                                TimeZone.UTC
                            )
                    )
                )
            )

            coEvery { mockSyncService.postQueueActions(any()) }.returns(DataResult.Success(Unit))
            every { mockSyncControlDatabaseHelper.completeActions(any()) }.returns(true)
            every { mockSyncUploadFileManager.syncFiles(any()) }.invokes {
                (it[0] as Callback).invoke()
            }
            every { mockUploadFileRepository.hasFilesToUpload() }.returns(false)

            with(HorusContainer) {
                setupMigrationService(mockMigrationService)
                setupNetworkValidator(mockNetworkValidator)
                setupSettings(mockSettings)
                setupLogger(KotlinLogger())
                setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
                setupOperationDatabaseHelper(mockOperationDatabaseHelper)
                setupRemoteSynchronizatorManager(
                    RemoteSynchronizatorManager(
                        mockNetworkValidator,
                        mockSyncControlDatabaseHelper,
                        mockSyncService,
                        mockUploadFileRepository
                    )
                )
                setupSyncFileUploadedManager(mockSyncUploadFileManager)
            }

            // When
            HorusDataFacade.forceSync(onSuccess = {
                invokedOnSuccess = true
            }, onFailure = {
                invokedOnFailure = true
            })

            // Then
            delay(500)
            verify { mockNetworkValidator.isNetworkAvailable() }
            Assert.assertFalse(invokedOnFailure)
            assert(invokedOnSuccess)
            Assert.assertEquals(0, EventBus.getCountListeners(EventType.SYNC_PUSH_FAILED))
            Assert.assertEquals(0, EventBus.getCountListeners(EventType.SYNC_PUSH_SUCCESS))
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
        validateInsertWithIdTest()
        validateInsertIsFailureByRestriction()
        validateInsertBatchTest()
        validateInsertBatchIsFailureByMaxCountRestriction()
        validateInsertBatchWithIdsTest()
        validateUpdateBatchIsTest()
        validateInsertAndUpdateIsSuccess()
        validateInsertAndDeleteIsSuccess()
        validateGetEntityByIdReturnRecord()
        validateGetByIdReturnNull()
        validateGetEntities()
        validateGetEntitiesWithWhereConditions()
        validateGetEntitiesWithLimitAndOffset()
        validateGetEntitiesName()
        whenUploadFileIsSuccess()
        whenGetFileUriNetworkIsNotAvailableThenReturnUrlLocal()
        whenGetFileUriNetworkIsNotAvailableThenReturnNull()
        whenGetFileUriNetworkIsAvailableThenReturnUrl()
        validateCountRecordFromEntity()
        validateCountRecordFromEntityWithConditions()
        validateCountRecordsWithConditions()
        validateQueryWithWhereLikeConditions()
        validateQueryWithWhereLikeConditionsAlternative()

        assert(invokedInsert)
        assert(invokedUpdate)
        assert(invokedDelete)
    }


    @Test
    fun `when uploadFile is Failure by is not ready`(): Unit = runBlocking {
        Assert.assertThrows(IllegalStateException::class.java) {
            HorusDataFacade.uploadFile(generateFileDataImage())
        }
    }

    private fun whenUploadFileIsSuccess() = prepareInternalTest {
        // Given
        val fileReference = Horus.FileReference()
        val fileData = generateFileDataImage()

        every { uploadFileRepository.createFileLocal(fileData) }.returns(fileReference)

        // When
        val result = HorusDataFacade.uploadFile(fileData)

        // Then
        Assert.assertEquals(fileReference, result)
    }

    private fun whenGetFileUriNetworkIsNotAvailableThenReturnUrlLocal(): Unit =
        prepareInternalTest {
            val fileReference = Horus.FileReference()
            val urlLocal = "local/path"

            every { networkValidator.isNetworkAvailable() }.returns(false)
            every { uploadFileRepository.getFileUrlLocal(fileReference) }.returns(urlLocal)

            // When
            val result = HorusDataFacade.getFileUri(fileReference)

            // Then
            Assert.assertEquals(urlLocal, result)
        }

    private fun whenGetFileUriNetworkIsNotAvailableThenReturnNull(): Unit = prepareInternalTest {
        val fileReference = Horus.FileReference()

        every { networkValidator.isNetworkAvailable() }.returns(false)
        every { uploadFileRepository.getFileUrlLocal(fileReference) }.returns(null)

        // When
        val result = HorusDataFacade.getFileUri(fileReference)

        // Then
        Assert.assertNull(result)
    }

    private fun whenGetFileUriNetworkIsAvailableThenReturnUrl(): Unit = prepareInternalTest {
        val fileReference = Horus.FileReference()
        val urlRemote = "remote/path"

        every { networkValidator.isNetworkAvailable() }.returns(true)
        coEvery { uploadFileRepository.getFileUrl(fileReference) }.returns(urlRemote)

        // When
        val result = HorusDataFacade.getFileUri(fileReference)

        // Then
        Assert.assertEquals(urlRemote, result)
    }

    private fun validateEntityIsNotWritable() = prepareInternalTest {
        Assert.assertThrows(EntityNotWritableException::class.java) {
            HorusDataFacade.insert("product_breeds", mapOf("key" to "value"))
        }
    }

    private suspend fun validatesOperationIsFailureByEntityNoExists() = prepareInternalTest {

        val entityName = "any_table_" + Random.nextInt()

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.insert(entityName, mapOf("key" to "value"))
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.update(entityName, "id", mapOf("key" to "value"))
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.delete(entityName, "id")
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.getById(entityName, "id")
        }

        Assert.assertThrows(EntityNotExistsException::class.java) {
            runBlocking {
                HorusDataFacade.querySimple(entityName)
            }
        }

    }

    private fun validateInsertTest() = prepareInternalTest {
        val result = HorusDataFacade.insert(
            "measures",
            createDataInsertRecord()
        )
        assert(result is DataResult.Success)
    }

    private fun validateInsertWithIdTest() = prepareInternalTest {
        val idExpected = uuid()
        val result = HorusDataFacade.insert(
            "measures",
            createDataInsertRecord().toMutableMap().apply {
                put("id", idExpected)
            }
        )
        assert(result is DataResult.Success)
        result.coFold(
            { id ->
                assertEquals(idExpected, id)
            },
            { exception ->
                fail(exception.message)
            })
    }

    private fun validateInsertIsFailureByRestriction() = prepareInternalTest {
        val idExpected = uuid()

        // Set restriction
        HorusDataFacade.setEntityRestrictions(
            listOf(
                MaxCountEntityRestriction("measures", 0)
            )
        )

        val result = HorusDataFacade.insert(
            "measures",
            createDataInsertRecord().toMutableMap().apply {
                put("id", idExpected)
            }
        )
        assert(result is DataResult.NotAuthorized)
        result.coFold(
            onSuccess = { id ->
                fail()
            },
            onFailure = { exception ->
                fail(exception.message)
            },
            onNotAuthorized = { assert(true) }
        )
    }

    private fun validateInsertBatchTest() = prepareInternalTest {

        HorusDataFacade.setEntityRestrictions(
            listOf(
                MaxCountEntityRestriction("measures", 3)
            )
        )

        val result = HorusDataFacade.insertBatch(
            listOf(
                Horus.Batch.Insert("measures", createDataInsertRecord()),
                Horus.Batch.Insert("measures", createDataInsertRecord()),
                Horus.Batch.Insert("measures", createDataInsertRecord())
            )
        )
        assert(result is DataResult.Success)
    }

    private fun validateInsertBatchIsFailureByMaxCountRestriction() = prepareInternalTest {

        // Set restriction
        HorusDataFacade.setEntityRestrictions(
            listOf(
                MaxCountEntityRestriction("measures", 2)
            )
        )
        val result = HorusDataFacade.insertBatch(
            listOf(
                Horus.Batch.Insert("measures", createDataInsertRecord()),
                Horus.Batch.Insert("measures", createDataInsertRecord()),
                Horus.Batch.Insert("measures", createDataInsertRecord())
            )
        )
        assert(result is DataResult.NotAuthorized)
    }


    private fun validateInsertBatchWithIdsTest() = prepareInternalTest {

        val result = HorusDataFacade.insertBatch(
            listOf(
                Horus.Batch.Insert("measures", createDataInsertRecordWithId()),
                Horus.Batch.Insert("measures", createDataInsertRecordWithId()),
                Horus.Batch.Insert("measures", createDataInsertRecordWithId())
            )
        )
        assert(result is DataResult.Success)
    }

    private fun validateUpdateBatchIsTest() = prepareInternalTest {

        val resultInsert = HorusDataFacade.insertBatch(
            listOf(
                Horus.Batch.Insert("measures", createDataInsertRecordWithId()),
                Horus.Batch.Insert("measures", createDataInsertRecordWithId()),
                Horus.Batch.Insert("measures", createDataInsertRecordWithId())
            )
        )

        val resultUpdate = HorusDataFacade.updateBatch(
            (resultInsert as DataResult.Success).data.map {
                Horus.Batch.Update(
                    "measures",
                    it,
                    listOf(
                        Horus.Attribute("value", Random.nextFloat()),
                        Horus.Attribute("nullable", null)
                    )
                )
            }
        )

        // Then
        assert(resultInsert is DataResult.Success)
        assert(resultUpdate is DataResult.Success)
    }

    private fun validateInsertAndUpdateIsSuccess() = prepareInternalTest {
        // Given
        val valueExpected = Random.nextFloat()
        val resultInsert = HorusDataFacade.insert(
            "measures",
            createDataInsertRecord()
        )

        // When

        val resultUpdate = if (resultInsert is DataResult.Success) {
            HorusDataFacade.update(
                "measures",
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
            assert(driver.rawQuery("SELECT * FROM measures WHERE id= '" + resultInsert.data + "' AND value = $valueExpected") {
                it.getString(0)
            }.isNotEmpty())
        } else {
            fail()
        }
    }

    private fun validateInsertAndDeleteIsSuccess() = prepareInternalTest {
        // Given
        val resultInsert = HorusDataFacade.insert(
            "measures",
            createDataInsertRecord()
        )

        // When
        val resultDelete = if (resultInsert is DataResult.Success) {
            HorusDataFacade.delete("measures", resultInsert.data)
        } else {
            DataResult.Failure(Exception("Error"))
        }

        // Then
        assert(resultInsert is DataResult.Success)
        assert(resultDelete is DataResult.Success)

        if (resultInsert is DataResult.Success) {
            assert(driver.rawQuery("SELECT * FROM measures WHERE id= '" + resultInsert.data + "'") {
                it.getString(0)
            }.isEmpty())
        } else {
            fail()
        }
    }

    private fun validateGetEntityByIdReturnRecord() = prepareInternalTest {
        // Given
        val resultInsert = HorusDataFacade.insert(
            "measures",
            createDataInsertRecord()
        )

        // When
        val entity =
            HorusDataFacade.getById(
                "measures",
                (resultInsert as DataResult.Success).data
            )

        // Then
        Assert.assertNotNull(entity)
        Assert.assertTrue((entity?.getDouble("value") ?: 0.0) > 0.0)
    }

    private suspend fun validateGetEntities() = prepareInternalTest {
        // Given
        val attributesList = generateRandomArray {
            createDataInsertRecord().map { Horus.Attribute(it.key, it.value) }
        }

        attributesList.forEach {
            HorusDataFacade.insert("measures", *it.toTypedArray())
        }

        // When
        val result =
            HorusDataFacade.querySimple("measures")

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

        val insertResult = HorusDataFacade.insert("measures", *attributesList.toTypedArray())

        // When

        val entityId = getEntityId(insertResult)
        val result =
            HorusDataFacade.querySimple(
                "measures",
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
            HorusDataFacade.insert("measures", *it.toTypedArray())
        }

        // When
        val result =
            HorusDataFacade.querySimple(
                "measures",
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

    private fun validateGetByIdReturnNull() {
        // When
        val entity =
            HorusDataFacade.getById("measures", uuid())
        // Then
        Assert.assertNull(entity)
    }

    private fun validateGetEntitiesName() = prepareInternalTest {
        // Given
        val countEntitiesExpected = 2

        // When
        val entitiesName = HorusDataFacade.getEntityNames()

        // Then
        Assert.assertTrue(entitiesName.isNotEmpty())
        Assert.assertEquals(countEntitiesExpected, entitiesName.size)
    }


    private suspend fun validateCountRecordFromEntity() = prepareInternalTest {
        // Given
        val entitiesAttributes = generateRandomArray {
            createDataInsertRecord().map { Horus.Attribute(it.key, it.value) }
        }

        entitiesAttributes.forEach {
            HorusDataFacade.insert("measures", *it.toTypedArray())
        }

        // When
        val result =
            HorusDataFacade.countRecordFromEntity("measures")

        result.fold(
            { count ->
                Assert.assertEquals(entitiesAttributes.size, count)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }

    private suspend fun validateCountRecordFromEntityWithConditions() = prepareInternalTest {
        // Given
        val entitiesAttributes = generateRandomArray {
            mapOf(
                "measure" to "w",
                "unit" to "kg",
                "value" to Random.nextBoolean(),
                "nullable" to null
            )
        }

        entitiesAttributes.forEach {
            HorusDataFacade.insert("measures", it)
        }

        // When
        val result =
            HorusDataFacade.countRecordFromEntity(
                "measures",
                SQL.WhereCondition(SQL.ColumnValue("value", true))
            )

        result.fold(
            { count ->
                Assert.assertEquals(entitiesAttributes.count { it["value"] == true }, count)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }


    private suspend fun validateCountRecordsWithConditions() = prepareInternalTest {
        // Given
        val entitiesAttributes = generateRandomArray(30) {
            mapOf(
                "measure" to "w",
                "unit" to "kg",
                "value" to Random.nextBoolean(),
                "nullable" to if (Random.nextBoolean()) null else Random.nextInt(1..1000)
            )
        }

        entitiesAttributes.forEach {
            HorusDataFacade.insert("measures", it)
        }

        // When
        val result =
            HorusDataFacade.countRecords(
                SimpleQueryBuilder("measures")
                    .where(SQL.WhereCondition(SQL.ColumnValue("value", true)))
                    .where(
                        SQL.WhereCondition(SQL.ColumnValue("nullable"), SQL.Comparator.IS_NULL),
                        joinOperator = SQL.LogicOperator.OR
                    ) as SimpleQueryBuilder
            )

        result.fold(
            { count ->
                Assert.assertEquals(entitiesAttributes.count { it["value"] == true || it["nullable"] == null }, count)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }

    private suspend fun validateQueryWithWhereLikeConditions() = prepareInternalTest {
        val entitiesAttributes = generateRandomArray {
            mapOf(
                "measure" to "w",
                "unit" to "kg",
                "nullable" to null,
                "value" to "John " + Random.nextUInt(),
            )
        }

        entitiesAttributes.forEach {
            HorusDataFacade.insert("measures", it)
        }

        val builder = SimpleQueryBuilder("measures").where(
            SQL.WhereCondition(SQL.ColumnValue("value", "John%"), SQL.Comparator.LIKE)
        )

        // When
        val result =
            HorusDataFacade.query(builder)

        // Then
        result.fold(
            { entities ->
                Assert.assertTrue(entities.isNotEmpty())
                Assert.assertEquals(entitiesAttributes.size, entities.size)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }

    private suspend fun validateQueryWithWhereLikeConditionsAlternative() = prepareInternalTest {
        val entitiesAttributes = generateRandomArray {
            mapOf(
                "measure" to "w",
                "unit" to "kg",
                "nullable" to null,
                "value" to Random.nextUInt().toString() + " John " + Random.nextUInt(),
            )
        }

        entitiesAttributes.forEach {
            HorusDataFacade.insert("measures", it)
        }

        val builder = SimpleQueryBuilder("measures").where(
            SQL.WhereCondition(SQL.ColumnValue("value", "%John%"), SQL.Comparator.LIKE)
        )

        // When
        val result = HorusDataFacade.query(builder)

        // Then
        result.fold(
            { entities ->
                Assert.assertTrue(entities.isNotEmpty())
                Assert.assertEquals(entitiesAttributes.size, entities.size)
            },
            { exception ->
                Assert.fail(exception.message)
            }
        )
    }

    //---------------------------------------------

    private fun createDataInsertRecord() = mapOf(
        "measure" to "w",
        "unit" to "kg",
        "value" to 10.0f,
        "nullable" to null
    )

    private fun createDataInsertRecordWithId() = mapOf(
        "id" to uuid(),
        "measure" to "w",
        "unit" to "kg",
        "value" to 10.0f,
        "nullable" to Random.nextInt(1..1000)
    )

    private fun prepareEnvironment(block: suspend () -> Unit) = runBlocking {
        HorusDataFacade
        EventBus.emit(EventType.ON_READY)
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        migrateDatabase()
        block()
    }

    private fun prepareInternalTest(block: suspend () -> Unit) = runBlocking {
        driver.execute("DELETE FROM measures")
        driver.execute("DELETE FROM product_breeds")
        HorusDataFacade.setEntityRestrictions(emptyList())
        block()
    }

    private fun migrateDatabase() {
        val entitiesSchema =
            buildEntitiesSchemeFromJSON(DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE).map { it.toScheme() }
        HorusDatabase.Schema.create(driver, entitiesSchema)

        driver.also {
            Assert.assertTrue(
                "table measures not exists",
                it.getTablesNames().contains("measures")
            )
        }
    }

    private fun getEntityId(insertResult: DataResult<String>): String {
        return (insertResult as DataResult.Success).data
    }


}