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
import org.apptank.horus.client.bus.InternalEventBus
import org.apptank.horus.client.bus.EventType
import org.apptank.horus.client.di.IDatabaseDriverFactory
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.exception.EntityNotExistsException
import org.apptank.horus.client.exception.EntityNotWritableException
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.migration.network.service.IMigrationService
import org.apptank.horus.client.migration.network.toScheme
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.coFold
import org.apptank.horus.client.base.encodeToJSON
import org.apptank.horus.client.control.QueueActionsTable
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.ActionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.encodeToString
import org.apptank.horus.client.serialization.AnySerializer
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.scheme.DataSharedTable
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.extensions.getRequireInt
import org.apptank.horus.client.restrictions.MaxCountEntityRestriction
import org.apptank.horus.client.sync.manager.ISyncFileUploadedManager
import org.apptank.horus.client.sync.manager.PushDataRemoteSynchronizatorManager
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.apptank.horus.client.tasks.RefreshReadableEntitiesTask
import org.apptank.horus.client.tasks.RetrieveDataSharedTask
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
import dev.mokkery.answering.returns
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import org.robolectric.annotation.Config
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextUInt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AndroidHorusDataFacadeTest : TestCase() {

    private lateinit var databaseFactory: IDatabaseDriverFactory
    private lateinit var context: Context
    private lateinit var driver: SqlDriver

        val networkValidator = mock<INetworkValidator>(MockMode.autofill)

        val migrationService = mock<IMigrationService>(MockMode.autofill)

        val synchronizationService = mock<ISynchronizationService>(MockMode.autofill)

        val uploadFileRepository = mock<IUploadFileRepository>(MockMode.autofill)

        val storageSettings = mock<Settings>(MockMode.autofill)

        val fileUploadManager = mock<ISyncFileUploadedManager>(MockMode.autofill)

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
        InternalEventBus.emit(EventType.ON_READY)
        assert(invoked)
    }

    @Test
    fun `validate method onReady when already is ready`() {
        var invoked = false
        HorusDataFacade.init()
        InternalEventBus.emit(EventType.ON_READY)
        HorusDataFacade.onReady {
            invoked = true
        }
        assert(invoked)
    }

    @Test
    fun `validate method onReady when already user token is setup`() {
        var invoked = false
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        InternalEventBus.emit(EventType.ON_READY)
        HorusDataFacade.onReady {
            invoked = true
        }
        assert(invoked)
    }

    @Test
    fun `validate clear database when user session is cleared`() {

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        HorusDataFacade.init()
        InternalEventBus.emit(EventType.ON_READY)

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

        verify { storageSettings.clear() }
    }

    @Test
    fun `when is not ready then throw exception because is not ready`(): Unit = runBlocking {
        coAssertThrows(IllegalStateException::class.java) {
            HorusDataFacade.insert("table", mapOf("key" to "value"))
        }
    }

    @Test
    fun `when hasDataToSync return true`(): Unit = runBlocking {
        // Given
        val mockSyncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
        val mockUploadFileRepository = mock<IUploadFileRepository>(MockMode.autofill)

        with(HorusContainer) {
            setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
            setupUploadFileRepository(mockUploadFileRepository)
        }

        every {
            mockSyncControlDatabaseHelper.getPendingActions()
        } returns (
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
        every { mockUploadFileRepository.hasFilesToUpload() } returns (false)

        // When
        val result = HorusDataFacade.hasDataToSync()
        // Then
        assert(result)
    }

    @Test
    fun `when hasDataToSync return false`(): Unit = runBlocking {
        // Given
        val mockSyncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
        val mockUploadFileRepository = mock<IUploadFileRepository>(MockMode.autofill)

        with(HorusContainer) {
            setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
            setupUploadFileRepository(mockUploadFileRepository)
        }

        every {
            mockSyncControlDatabaseHelper.getPendingActions()
        } returns (emptyList())

        every { mockUploadFileRepository.hasFilesToUpload() } returns false

        // When
        val result = HorusDataFacade.hasDataToSync()
        // Then
        Assert.assertFalse(result)
    }

    @Test
    fun `when getLastSyncDate return null`(): Unit = runBlocking {
        // Given
        val mockSyncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
        val timestampExpected = Clock.System.now().epochSeconds

        with(HorusContainer) {
            setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
        }

        every {
            mockSyncControlDatabaseHelper.getLastDatetimeCheckpoint()
        } returns (timestampExpected)

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
            val mockNetworkValidator = mock<INetworkValidator>(MockMode.autofill)
            every { mockNetworkValidator.isNetworkAvailable() } returns (false)
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

            val mockNetworkValidator = mock<INetworkValidator>(MockMode.autofill)
            val mockMigrationService = mock<IMigrationService>(MockMode.autofill)
            val mockSyncService = mock<ISynchronizationService>(MockMode.autofill)
            val mockSettings = mock<Settings>(MockMode.autofill)
            val mockSyncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
            val mockOperationDatabaseHelper = mock<IOperationDatabaseHelper>(MockMode.autofill)
            val mockSyncUploadFileManager = mock<ISyncFileUploadedManager>(MockMode.autofill)
            val mockUploadFileRepository = mock<IUploadFileRepository>(MockMode.autofill)

            HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
            HorusContainer.setupLogger(KotlinLogger())

            every { mockNetworkValidator.isNetworkAvailable() } returns (true)
            everySuspend { mockMigrationService.getMigration() } returns (
                DataResult.Success(
                    buildEntitiesSchemeFromJSON(DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE)
                )
            )
            every {
                mockSettings.getLongOrNull(ValidateMigrationLocalDatabaseTask.KEY_SCHEMA_VERSION)
            } returns (1)

            every { mockSettings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) } returns (
                Clock.System.now().epochSeconds - 1)

            every { mockSettings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) } returns (
                Clock.System.now().epochSeconds - 1)

            every {
                mockSyncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION)
            } returns (true)

            every {
                mockSyncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
            } returns (true)

            every {
                mockSyncControlDatabaseHelper.getPendingActions()
            } returns (
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

            every { mockSyncControlDatabaseHelper.getCompletedActionsAfterDatetime(any()) } returns emptyList()
            every { mockSyncControlDatabaseHelper.getExistsActionSequences(any()) } returns emptyList()
            everySuspend { synchronizationService.getQueueActions(any()) } returns DataResult.Success(emptyList())
            every { mockSyncControlDatabaseHelper.getWritableEntityNames() }.returns(emptyList())

            everySuspend { mockSyncService.postQueueActions(any()) } returns (DataResult.Success(Unit))
            every { mockSyncControlDatabaseHelper.completeActions(any()) } returns (true)
            every { mockSyncUploadFileManager.syncFiles(any()) } calls { args ->
                (args.args[0] as Callback).invoke()
            }
            every { mockUploadFileRepository.hasFilesToUpload() } returns (false)

            with(HorusContainer) {
                setupMigrationService(mockMigrationService)
                setupNetworkValidator(mockNetworkValidator)
                setupSettings(mockSettings)
                setupLogger(KotlinLogger())
                setupSyncControlDatabaseHelper(mockSyncControlDatabaseHelper)
                setupOperationDatabaseHelper(mockOperationDatabaseHelper)
                setupRemoteSynchronizatorManager(
                    PushDataRemoteSynchronizatorManager(
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
            Assert.assertEquals(0, InternalEventBus.getCountListeners(EventType.SYNC_PUSH_FAILED))
            Assert.assertEquals(0, InternalEventBus.getCountListeners(EventType.SYNC_PUSH_SUCCESS))
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
        validateInsertBatchWithRelatedTest()
        validateInsertBatchIsFailureByMaxCountRestriction()
        validateInsertBatchWithIdsTest()
        validateUpdateBatchIsTest()
        validateInsertAndUpdateIsSuccess()
        validateInsertAndDeleteIsSuccess()
        validateExecuteBatchOperations()
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
        validateQueryExists()
        queryDataSharedSuccess()
        validateSyncFailedEventWhenForceInitialSync()

        assert(invokedInsert)
        assert(invokedUpdate)
        assert(invokedDelete)
    }


    @Test
    fun `when uploadFile is Failure by is not ready`(): Unit = runBlocking {
        coAssertThrows(IllegalStateException::class.java) {
            HorusDataFacade.uploadFile(generateFileDataImage())
        }
    }

    private fun whenUploadFileIsSuccess() = prepareInternalTest {
        // Given
        val fileReference = Horus.FileReference()
        val fileData = generateFileDataImage()

        every { uploadFileRepository.createFileLocal(fileData) } returns (fileReference)

        // When
        val result = HorusDataFacade.uploadFile(fileData)

        // Then
        Assert.assertEquals(fileReference, result)
    }

    private fun whenGetFileUriNetworkIsNotAvailableThenReturnUrlLocal(): Unit =
        prepareInternalTest {
            val fileReference = Horus.FileReference()
            val urlLocal = "local/path"

            every { networkValidator.isNetworkAvailable() } returns (false)
            every { uploadFileRepository.getFileUrlLocal(fileReference) } returns (urlLocal)

            // When
            val result = HorusDataFacade.getFileUri(fileReference)

            // Then
            Assert.assertEquals(urlLocal, result)
        }

    private fun whenGetFileUriNetworkIsNotAvailableThenReturnNull(): Unit = prepareInternalTest {
        val fileReference = Horus.FileReference()

        every { networkValidator.isNetworkAvailable() } returns (false)
        every { uploadFileRepository.getFileUrlLocal(fileReference) } returns (null)

        // When
        val result = HorusDataFacade.getFileUri(fileReference)

        // Then
        Assert.assertNull(result)
    }

    private fun whenGetFileUriNetworkIsAvailableThenReturnUrl(): Unit = prepareInternalTest {
        val fileReference = Horus.FileReference()
        val urlRemote = "remote/path"

        every { networkValidator.isNetworkAvailable() } returns (true)
        everySuspend { uploadFileRepository.getFileUrl(fileReference) } returns (urlRemote)

        // When
        val result = HorusDataFacade.getFileUri(fileReference)

        // Then
        Assert.assertEquals(urlRemote, result)
    }

    private fun validateEntityIsNotWritable() = prepareInternalTest {
        coAssertThrows(EntityNotWritableException::class.java) {
            HorusDataFacade.insert("product_breeds", mapOf("key" to "value"))
        }
    }

    private suspend fun validatesOperationIsFailureByEntityNoExists() = prepareInternalTest {

        val entityName = "any_table_" + Random.nextInt()

        coAssertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.insert(entityName, mapOf("key" to "value"))
        }

        coAssertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.update(entityName, "id", mapOf("key" to "value"))
        }

        coAssertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.delete(entityName, "id")
        }

        coAssertThrows(EntityNotExistsException::class.java) {
            HorusDataFacade.getById(entityName, "id")
        }

        coAssertThrows(EntityNotExistsException::class.java) {
            runBlocking {
                HorusDataFacade.querySimple(entityName)
            }
        }

    }

    private fun validateInsertTest() = prepareInternalTest {
        val result = HorusDataFacade.insert(
            "measures",
            createDataMeasureRecord()
        )
        assert(result is DataResult.Success)
    }

    private fun validateInsertWithIdTest() = prepareInternalTest {
        val idExpected = uuid()
        val result = HorusDataFacade.insert(
            "measures",
            createDataMeasureRecord().toMutableMap().apply {
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
            createDataMeasureRecord().toMutableMap().apply {
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
                Horus.Batch.Insert("measures", createDataMeasureRecord()),
                Horus.Batch.Insert("measures", createDataMeasureRecord()),
                Horus.Batch.Insert("measures", createDataMeasureRecord())
            )
        )
        assert(result is DataResult.Success)
    }

    private fun validateInsertBatchWithRelatedTest() = prepareInternalTest {

        // Given
        val result =
            HorusDataFacade.insert("measures", createDataMeasureRecord()) as DataResult.Success

        val measureId = result.data
        val measureMetadata = createDataMeasureMetadataRecord(
            measureId = measureId
        )

        val userOwnerId = driver.rawQuery("SELECT * FROM measures WHERE id= '$measureId'") {
            it.getString(1)
        }[0]

        assertEquals(HorusAuthentication.getUserAuthenticatedId(), userOwnerId)
        // When
        HorusDataFacade.insertBatch(
            listOf(
                Horus.Batch.Insert("measures_metadata", measureMetadata)
            )
        )

        // Then
        assert(
            driver.rawQuery(
                "SELECT * FROM measures_metadata WHERE measure_id = '$measureId' AND ${Horus.Attribute.OWNER_ID} = '$userOwnerId'"
            ) {
                it.getString(0)
            }.isNotEmpty()
        )
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
                Horus.Batch.Insert("measures", createDataMeasureRecord()),
                Horus.Batch.Insert("measures", createDataMeasureRecord()),
                Horus.Batch.Insert("measures", createDataMeasureRecord())
            )
        )
        assert(result is DataResult.NotAuthorized)
    }


    private fun validateInsertBatchWithIdsTest() = prepareInternalTest {

        val result = HorusDataFacade.insertBatch(
            listOf(
                Horus.Batch.Insert("measures", createDataMeasureRecordWithId()),
                Horus.Batch.Insert("measures", createDataMeasureRecordWithId()),
                Horus.Batch.Insert("measures", createDataMeasureRecordWithId())
            )
        )
        assert(result is DataResult.Success)
    }

    private fun validateUpdateBatchIsTest() = prepareInternalTest {

        val resultInsert = HorusDataFacade.insertBatch(
            listOf(
                Horus.Batch.Insert("measures", createDataMeasureRecordWithId()),
                Horus.Batch.Insert("measures", createDataMeasureRecordWithId()),
                Horus.Batch.Insert("measures", createDataMeasureRecordWithId())
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
            createDataMeasureRecord()
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
            createDataMeasureRecord()
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

    private fun validateExecuteBatchOperations() = prepareInternalTest {
        // Given
        val measure = createDataMeasureRecordWithId()
        val measureId = measure["id"] as String
        val operations = listOf(
            Horus.Batch.Insert("measures", measure),
            Horus.Batch.Update(
                "measures",
                measureId,
                listOf(Horus.Attribute("value", Random.nextFloat()))
            ),
            Horus.Batch.Delete("measures", measureId)
        )

        // When
        val result = HorusDataFacade.executeBatchOperations(operations)

        // Then
        assert(result is DataResult.Success)
        if (result is DataResult.Success) {
            assert(driver.rawQuery("SELECT * FROM measures") {
                it.getString(0)
            }.isEmpty())
            assertEquals(3, driver.rawQuery("SELECT * FROM ${QueueActionsTable.TABLE_NAME}") {
                it.getString(0)
            }.size)
        } else {
            fail()
        }
    }

    private fun validateGetEntityByIdReturnRecord() = prepareInternalTest {
        // Given
        val resultInsert = HorusDataFacade.insert(
            "measures",
            createDataMeasureRecord()
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
            createDataMeasureRecord().map { Horus.Attribute(it.key, it.value) }
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
        val attributesList = createDataMeasureRecord().map { Horus.Attribute(it.key, it.value) }

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
            createDataMeasureRecord().map { Horus.Attribute(it.key, it.value) }
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
        val countEntitiesExpected = 3

        // When
        val entitiesName = HorusDataFacade.getEntityNames()

        // Then
        Assert.assertTrue(entitiesName.isNotEmpty())
        Assert.assertEquals(countEntitiesExpected, entitiesName.size)
    }


    private suspend fun validateCountRecordFromEntity() = prepareInternalTest {
        // Given
        val entitiesAttributes = generateRandomArray {
            createDataMeasureRecord().map { Horus.Attribute(it.key, it.value) }
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
                Assert.assertEquals(
                    entitiesAttributes.count { it["value"] == true || it["nullable"] == null },
                    count
                )
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

    private suspend fun validateQueryExists() = prepareInternalTest {
        // Given
        val measure = createDataMeasureRecord()
        val resultInsert = HorusDataFacade.insert("measures", measure)
        val id = (resultInsert as DataResult.Success).data

        // When - Query for existing record
        val builderExists = SimpleQueryBuilder("measures").where(
            SQL.WhereCondition(SQL.ColumnValue("id", id))
        )
        val resultExists = HorusDataFacade.queryExists(builderExists)

        // When - Query for non-existing record
        val builderNotExists = SimpleQueryBuilder("measures").where(
            SQL.WhereCondition(SQL.ColumnValue("id", "non-existent-id"))
        )
        val resultNotExists = HorusDataFacade.queryExists(builderNotExists)

        // Then
        resultExists.fold(
            { exists -> Assert.assertTrue("Record should exist", exists) },
            { fail(it.message) }
        )
        resultNotExists.fold(
            { exists -> Assert.assertFalse("Record should not exist", exists) },
            { fail(it.message) }
        )
    }

    private suspend fun queryDataSharedSuccess() = prepareInternalTest {

        val entityName = "test_entity"

        for (i in 1..5) {
            val entityId = "id_$i" // Use predictable IDs for testing order
            val data = mapOf(
                "index" to "value$i",
                "attr1" to "value2$i",
                "float" to Random.nextFloat(),
                "int" to Random.nextInt(1..1000),
                "bool" to Random.nextBoolean(),
            )
            driver.insertOrThrow(
                DataSharedTable.TABLE_NAME, mapOf(
                    DataSharedTable.ATTR_ID to entityId,
                    DataSharedTable.ATTR_ENTITY_NAME to entityName,
                    DataSharedTable.ATTR_DATA to data.encodeToJSON()
                )
            )
        }

        // Then
        val results = HorusDataFacade.queryDataShared(
            entityName,
            attributes = listOf(Horus.Attribute("attr1", "value23"))
        )

        // Then
        Assert.assertEquals(1, results.size)
        Assert.assertEquals("test_entity", results[0].name)
        Assert.assertEquals("value23", results[0].getRequireString("attr1"))
    }

    private fun validateSyncFailedEventWhenForceInitialSync() = prepareInternalTest {
        // Given

        val mockNetworkValidator = mock<INetworkValidator>(MockMode.autofill)
        every { mockNetworkValidator.isNetworkAvailable() } returns (false)
        HorusContainer.setupNetworkValidator(mockNetworkValidator)

        // When
        val result = HorusDataFacade.forceInitialSynchronization { }

        // Then
        assertFalse(result)
    }

    @Test
    fun `queryQueueActions returns empty list when no actions exist`(): Unit = runBlocking {
        // Given
        prepareEnvironment {
            driver.execute("DELETE FROM ${QueueActionsTable.TABLE_NAME}")
            val entityName = "measures"
            val minDate = LocalDate(2026, 5, 22)
            val timeZone = TimeZone.of("America/Bogota")

            // When
            val result = HorusDataFacade.queryQueueActions(
                entityNames = listOf(entityName),
                dataFilter = emptyMap(),
                minDate = minDate,
                maxDate = null,
                timeZone = timeZone
            )

            // Then
            result.fold(
                { queueActions ->
                    Assert.assertTrue(queueActions.isEmpty())
                },
                { exception ->
                    Assert.fail(exception.message)
                }
            )
        }
    }

    @Test
    fun `queryQueueActions returns actions filtered by entity name`(): Unit = runBlocking {
        // Given
        prepareEnvironment {
            driver.execute("DELETE FROM ${QueueActionsTable.TABLE_NAME}")
            val entity1 = "measures"
            val entity2 = "product_breeds"
            val timeZone = TimeZone.of("America/Bogota")
            val testDate = LocalDate(2026, 5, 22)
            val epoch = testDate.atTime(12, 0).toInstant(timeZone).epochSeconds

            // Insert actions in queue table
            val row1 = mapOf(
                QueueActionsTable.ATTR_ENTITY to entity1,
                QueueActionsTable.ATTR_ACTION_TYPE to SyncControl.ActionType.INSERT.id,
                QueueActionsTable.ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
                QueueActionsTable.ATTR_DATA to AnySerializer.decoderJSON.encodeToString(mapOf("id" to "1", "measure" to "w")),
                QueueActionsTable.ATTR_DATETIME to epoch
            )
            val row2 = mapOf(
                QueueActionsTable.ATTR_ENTITY to entity2,
                QueueActionsTable.ATTR_ACTION_TYPE to SyncControl.ActionType.UPDATE.id,
                QueueActionsTable.ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
                QueueActionsTable.ATTR_DATA to AnySerializer.decoderJSON.encodeToString(mapOf("id" to "2", "name" to "breed1")),
                QueueActionsTable.ATTR_DATETIME to epoch
            )

            driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
            driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)

            // When: Query only entity1
            val result = HorusDataFacade.queryQueueActions(
                entityNames = listOf(entity1),
                dataFilter = emptyMap(),
                minDate = testDate,
                maxDate = null,
                timeZone = timeZone
            )

            // Then
            result.fold(
                { queueActions ->
                    Assert.assertEquals(1, queueActions.size)
                    Assert.assertEquals(entity1, queueActions[0].entity)
                    Assert.assertEquals("1", queueActions[0].id)
                    Assert.assertEquals(ActionType.INSERT, queueActions[0].type)
                },
                { exception ->
                    Assert.fail(exception.message)
                }
            )
        }
    }

    @Test
    fun `queryQueueActions filters by multiple entity names`(): Unit = runBlocking {
        // Given
        prepareEnvironment {
            driver.execute("DELETE FROM ${QueueActionsTable.TABLE_NAME}")
            val entity1 = "measures"
            val entity2 = "product_breeds"
            val entity3 = "other_entity"
            val timeZone = TimeZone.of("America/Bogota")
            val testDate = LocalDate(2026, 5, 22)
            val epoch = testDate.atTime(12, 0).toInstant(timeZone).epochSeconds

            // Insert 3 actions for different entities
            listOf(
                Triple(entity1, SyncControl.ActionType.INSERT, "1"),
                Triple(entity2, SyncControl.ActionType.UPDATE, "2"),
                Triple(entity3, SyncControl.ActionType.DELETE, "3")
            ).forEach { (entity, actionType, id) ->
                val row = mapOf(
                    QueueActionsTable.ATTR_ENTITY to entity,
                    QueueActionsTable.ATTR_ACTION_TYPE to actionType.id,
                    QueueActionsTable.ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
                    QueueActionsTable.ATTR_DATA to AnySerializer.decoderJSON.encodeToString(mapOf("id" to id, "name" to "test")),
                    QueueActionsTable.ATTR_DATETIME to epoch
                )
                driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row)
            }

            // When: Query entities 1 and 2
            val result = HorusDataFacade.queryQueueActions(
                entityNames = listOf(entity1, entity2),
                dataFilter = emptyMap(),
                minDate = testDate,
                maxDate = null,
                timeZone = timeZone
            )

            // Then
            result.fold(
                { queueActions ->
                    Assert.assertEquals(2, queueActions.size)
                    val entityNames = queueActions.map { it.entity }
                    Assert.assertTrue(entityNames.contains(entity1))
                    Assert.assertTrue(entityNames.contains(entity2))
                    Assert.assertFalse(entityNames.contains(entity3))
                },
                { exception ->
                    Assert.fail(exception.message)
                }
            )
        }
    }

    @Test
    fun `queryQueueActions filters by date range`(): Unit = runBlocking {
        // Given
        prepareEnvironment {
            driver.execute("DELETE FROM ${QueueActionsTable.TABLE_NAME}")
            val entity = "measures"
            val timeZone = TimeZone.of("America/Bogota")

            // Create actions on different dates
            val dates = listOf(
                LocalDate(2026, 5, 20),
                LocalDate(2026, 5, 21),
                LocalDate(2026, 5, 22),
                LocalDate(2026, 5, 23)
            )

            dates.forEach { date ->
                val epoch = date.atTime(12, 0).toInstant(timeZone).epochSeconds
                val row = mapOf(
                    QueueActionsTable.ATTR_ENTITY to entity,
                    QueueActionsTable.ATTR_ACTION_TYPE to SyncControl.ActionType.INSERT.id,
                    QueueActionsTable.ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
                    QueueActionsTable.ATTR_DATA to AnySerializer.decoderJSON.encodeToString(mapOf("id" to "id_${date.dayOfMonth}", "measure" to "w")),
                    QueueActionsTable.ATTR_DATETIME to epoch
                )
                driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row)
            }

            // When: Query between 2026-05-21 and 2026-05-23 (inclusive)
            val minDate = LocalDate(2026, 5, 21)
            val maxDate = LocalDate(2026, 5, 23)

            val result = HorusDataFacade.queryQueueActions(
                entityNames = listOf(entity),
                dataFilter = emptyMap(),
                minDate = minDate,
                maxDate = maxDate,
                timeZone = timeZone
            )

            // Then: Should return 3 actions (21, 22, 23)
            result.fold(
                { queueActions ->
                    Assert.assertEquals(3, queueActions.size)
                    val ids = queueActions.map { it.id }
                    Assert.assertTrue(ids.contains("id_21"))
                    Assert.assertTrue(ids.contains("id_22"))
                    Assert.assertTrue(ids.contains("id_23"))
                    Assert.assertFalse(ids.contains("id_20"))
                },
                { exception ->
                    Assert.fail(exception.message)
                }
            )
        }
    }

    @Test
    fun `queryQueueActions filters by minimum date only`(): Unit = runBlocking {
        // Given
        prepareEnvironment {
            driver.execute("DELETE FROM ${QueueActionsTable.TABLE_NAME}")
            val entity = "measures"
            val timeZone = TimeZone.of("America/Bogota")

            val dates = listOf(
                LocalDate(2026, 5, 20),
                LocalDate(2026, 5, 21),
                LocalDate(2026, 5, 22)
            )

            dates.forEach { date ->
                val epoch = date.atTime(12, 0).toInstant(timeZone).epochSeconds
                val row = mapOf(
                    QueueActionsTable.ATTR_ENTITY to entity,
                    QueueActionsTable.ATTR_ACTION_TYPE to SyncControl.ActionType.INSERT.id,
                    QueueActionsTable.ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
                    QueueActionsTable.ATTR_DATA to AnySerializer.decoderJSON.encodeToString(mapOf("id" to "id_${date.dayOfMonth}", "measure" to "w")),
                    QueueActionsTable.ATTR_DATETIME to epoch
                )
                driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row)
            }

            // When: Query with minDate=21 and no maxDate.
            // When maxDate is null, the implementation uses minDate as maxDate (queries only that exact day)
            val minDate = LocalDate(2026, 5, 21)

            val result = HorusDataFacade.queryQueueActions(
                entityNames = listOf(entity),
                dataFilter = emptyMap(),
                minDate = minDate,
                maxDate = null,
                timeZone = timeZone
            )

            // Then: Should return only the action on May 21 (maxDate defaults to minDate)
            result.fold(
                { queueActions ->
                    Assert.assertEquals(1, queueActions.size)
                    Assert.assertEquals("id_21", queueActions[0].id)
                },
                { exception ->
                    Assert.fail(exception.message)
                }
            )
        }
    }

    @Test
    fun `queryQueueActions returns different action types correctly`(): Unit = runBlocking {
        // Given
        prepareEnvironment {
            driver.execute("DELETE FROM ${QueueActionsTable.TABLE_NAME}")
            val entity = "measures"
            val timeZone = TimeZone.of("America/Bogota")
            val testDate = LocalDate(2026, 5, 22)
            val epoch = testDate.atTime(12, 0).toInstant(timeZone).epochSeconds

            // Insert actions with different types
            val actionTypes = listOf(
                SyncControl.ActionType.INSERT,
                SyncControl.ActionType.UPDATE,
                SyncControl.ActionType.DELETE,
                SyncControl.ActionType.MOVE
            )

            actionTypes.forEachIndexed { index, actionType ->
                val row = mapOf(
                    QueueActionsTable.ATTR_ENTITY to entity,
                    QueueActionsTable.ATTR_ACTION_TYPE to actionType.id,
                    QueueActionsTable.ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
                    QueueActionsTable.ATTR_DATA to AnySerializer.decoderJSON.encodeToString(mapOf("id" to "id_$index", "measure" to "w")),
                    QueueActionsTable.ATTR_DATETIME to epoch
                )
                driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row)
            }

            // When
            val result = HorusDataFacade.queryQueueActions(
                entityNames = listOf(entity),
                dataFilter = emptyMap(),
                minDate = testDate,
                maxDate = null,
                timeZone = timeZone
            )

            // Then
            result.fold(
                { queueActions ->
                    Assert.assertEquals(4, queueActions.size)
                    Assert.assertTrue(queueActions.any { it.type == ActionType.INSERT })
                    Assert.assertTrue(queueActions.any { it.type == ActionType.UPDATE })
                    Assert.assertTrue(queueActions.any { it.type == ActionType.DELETE })
                    Assert.assertTrue(queueActions.any { it.type == ActionType.MOVE })
                },
                { exception ->
                    Assert.fail(exception.message)
                }
            )
        }
    }

    //---------------------------------------------

    private fun createDataMeasureRecord() = mapOf(
        "measure" to "w",
        "unit" to "kg",
        "value" to 10.0f,
        "nullable" to null,
        "point" to Random.nextFloat().toString() + "," + Random.nextFloat()
    )

    private fun createDataMeasureMetadataRecord(measureId: String) = mapOf(
        "id" to uuid(),
        "measure_id" to measureId,
        "name" to "name123" + Random.nextInt(1..1000),
        "value" to "value123" + Random.nextInt(1..1000),
    )

    private fun createDataMeasureRecordWithId() = mapOf(
        "id" to uuid(),
        "measure" to "w",
        "unit" to "kg",
        "value" to 10.0f,
        "nullable" to Random.nextInt(1..1000)
    )

    private fun prepareEnvironment(block: suspend () -> Unit) = runBlocking {
        HorusDataFacade
        InternalEventBus.emit(EventType.ON_READY)
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        migrateDatabase()
        block()
    }

    private fun prepareInternalTest(block: suspend () -> Unit) = runBlocking {
        driver.execute("DELETE FROM measures")
        driver.execute("DELETE FROM product_breeds")
        driver.execute("DELETE FROM measures_metadata")
        driver.execute("DELETE FROM ${QueueActionsTable.TABLE_NAME}")
        driver.execute("DELETE FROM ${DataSharedTable.TABLE_NAME}")
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