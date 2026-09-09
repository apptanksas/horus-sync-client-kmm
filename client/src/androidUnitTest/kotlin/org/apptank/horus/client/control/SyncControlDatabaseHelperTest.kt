package org.apptank.horus.client.control

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.control.scheme.SyncControlTable
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.cache.MemoryCache
import org.apptank.horus.client.control.scheme.DataSharedTable
import org.apptank.horus.client.control.scheme.EntitiesTable
import org.apptank.horus.client.control.scheme.EntityAttributesTable
import org.apptank.horus.client.control.scheme.QueueActionsSequenceTable
import org.apptank.horus.client.control.scheme.SyncFileTable
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.database.SyncControlDatabaseHelper
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.migration.domain.AttributeType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import org.apptank.horus.client.control.QueueActionsTable.ATTR_ACTION_TYPE
import org.apptank.horus.client.control.QueueActionsTable.ATTR_DATA
import org.apptank.horus.client.control.QueueActionsTable.ATTR_DATETIME
import org.apptank.horus.client.control.QueueActionsTable.ATTR_ENTITY
import org.apptank.horus.client.control.QueueActionsTable.ATTR_STATUS
import org.apptank.horus.client.control.QueueActionsTable.ATTR_EVENT_ID
import org.apptank.horus.client.serialization.AnySerializer
import kotlin.test.fail


class SyncControlDatabaseHelperTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var controlManagerDatabaseHelper: SyncControlDatabaseHelper

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        controlManagerDatabaseHelper = SyncControlDatabaseHelper("database", driver)

        HorusDatabase.Schema.create(driver)
        MemoryCache.flushCache()
    }


    @Test
    fun onCreateIsSuccess() {
        // Then
        val tablesNames = controlManagerDatabaseHelper.getTables()

        Assert.assertTrue(tablesNames.contains(SyncControlTable.TABLE_NAME))
        Assert.assertTrue(tablesNames.contains(QueueActionsTable.TABLE_NAME))
        Assert.assertTrue(tablesNames.contains(EntitiesTable.TABLE_NAME))
        Assert.assertTrue(tablesNames.contains(EntityAttributesTable.TABLE_NAME))
        Assert.assertTrue(tablesNames.contains(SyncFileTable.TABLE_NAME))
        Assert.assertTrue(tablesNames.contains(DataSharedTable.TABLE_NAME))
    }

    @Test
    fun isStatusCompletedIsTrue() {
        // Given
        driver.insertOrThrow(
            SyncControlTable.TABLE_NAME,
            SyncControlTable.mapToCreate(
                SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
                SyncControl.Status.COMPLETED
            )
        )
        // When
        val isStatusCompleted =
            controlManagerDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
        // Then
        Assert.assertTrue(isStatusCompleted)
    }

    @Test
    fun isStatusCompletedIsFalse() {
        // Given
        driver.insertOrThrow(
            SyncControlTable.TABLE_NAME,
            SyncControlTable.mapToCreate(
                SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
                SyncControl.Status.FAILED
            )
        )
        // When
        val isStatusCompleted =
            controlManagerDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
        // Then
        Assert.assertFalse(isStatusCompleted)
    }

    @Test
    fun getLastDatetimeCheckPointIsSuccess() {
        // Given
        val datetime = 123456789L
        driver.insertOrThrow(
            SyncControlTable.TABLE_NAME,
            SyncControlTable.mapToCreate(
                SyncControl.OperationType.CHECKPOINT,
                SyncControl.Status.COMPLETED
            ).plus(Pair(SyncControlTable.ATTR_DATETIME, datetime))
        )
        // When
        val lastDatetimeCheckpoint = controlManagerDatabaseHelper.getLastDatetimeCheckpoint()
        // Then
        Assert.assertEquals(datetime, lastDatetimeCheckpoint)
    }

    @Test
    fun getLastDatetimeCheckPointIsSuccessWithInitialSync() {
        // Given
        val datetime = 123456789L
        driver.insertOrThrow(
            SyncControlTable.TABLE_NAME,
            SyncControlTable.mapToCreate(
                SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
                SyncControl.Status.COMPLETED
            ).plus(Pair(SyncControlTable.ATTR_DATETIME, datetime))
        )
        // When
        val lastDatetimeCheckpoint = controlManagerDatabaseHelper.getLastDatetimeCheckpoint()
        // Then
        Assert.assertEquals(datetime, lastDatetimeCheckpoint)
    }


    @Test
    fun getLastDatetimeCheckPointWithAddSyncTypeStatusIsSuccess() {
        // Given

        // When
        controlManagerDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.CHECKPOINT,
            SyncControl.Status.COMPLETED
        )
        val lastDatetimeCheckpoint = controlManagerDatabaseHelper.getLastDatetimeCheckpoint()
        // Then
        Assert.assertNotEquals(0L, lastDatetimeCheckpoint)
    }

    @Test
    fun getLastDatetimeCheckPointWithAddSyncTypeStatusIsSuccessWithType() {
        controlManagerDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
            SyncControl.Status.COMPLETED
        )

        // When
        val lastDatetimeCheckpoint =
            controlManagerDatabaseHelper.getLastDatetimeCheckpoint(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
        // Then
        Assert.assertNotEquals(0L, lastDatetimeCheckpoint)
    }


    @Test
    fun getLastDatetimeCheckPointIsZero() {
        // When
        val lastDatetimeCheckpoint = controlManagerDatabaseHelper.getLastDatetimeCheckpoint()
        // Then
        Assert.assertEquals(0, lastDatetimeCheckpoint)
    }

    @Test
    fun addSyncTypeStatusWithCheckPointIsSuccess() {
        // When
        controlManagerDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.CHECKPOINT,
            SyncControl.Status.COMPLETED
        )
        // Then
        val isStatusCompleted =
            controlManagerDatabaseHelper.isStatusCompleted(SyncControl.OperationType.CHECKPOINT)
        Assert.assertTrue(isStatusCompleted)
    }

    @Test
    fun addSyncTypeStatusWithInitialIsSuccess() {
        // When
        controlManagerDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
            SyncControl.Status.COMPLETED
        )
        // Then
        val isStatusCompleted =
            controlManagerDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
        Assert.assertTrue(isStatusCompleted)
    }


    @Test
    fun addActionInsertIsSuccess() {
        // Given
        val entity = "entity123"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
        driver.registerEntity(entity)

        // When
        controlManagerDatabaseHelper.addActionInsert(entity, attributes)
        // Then
        val result = driver.executeQuery(
            null,
            "SELECT ${QueueActionsTable.ATTR_ENTITY} FROM ${QueueActionsTable.TABLE_NAME}",
            {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value
        Assert.assertEquals(entity, result)
    }

    @Test
    fun addActionInsertValidatingEventIdIsSuccess() {
        // Given
        val entity = "entity123"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
        driver.registerEntity(entity)

        // When
        controlManagerDatabaseHelper.addActionInsert(entity, attributes)
        // Then
        val result = driver.executeQuery(
            null,
            "SELECT ${QueueActionsTable.ATTR_EVENT_ID} FROM ${QueueActionsTable.TABLE_NAME}",
            {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value
        Assert.assertNotNull(result)
    }

    @Test
    fun addActionUpdateIsSuccess() {
        // Given
        val entity = "entity123"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
        driver.registerEntity(entity)

        // When
        controlManagerDatabaseHelper.addActionUpdate(entity, attributes.first(), attributes)

        // Then
        val result = driver.executeQuery(
            null,
            "SELECT ${QueueActionsTable.ATTR_ENTITY} FROM ${QueueActionsTable.TABLE_NAME}",
            {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value
        Assert.assertEquals(entity, result)
    }

    @Test
    fun addActionDeleteIsSuccess() {
        // Given
        val entity = "entity123"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
        driver.registerEntity(entity)

        // When
        controlManagerDatabaseHelper.addActionDelete(entity, attributes.first())

        // Then
        val result = driver.executeQuery(
            null,
            "SELECT ${QueueActionsTable.ATTR_ENTITY} FROM ${QueueActionsTable.TABLE_NAME}",
            {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value
        Assert.assertEquals(entity, result)
    }

    @Test
    fun addActionsCompletedIsSuccess() {
        // Given
        val entity = "entity_completed_123"
        val datetime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
        driver.registerEntity(entity)

        val actions = listOf(
            SyncControl.Action(
                id = 0,
                action = SyncControl.ActionType.INSERT,
                entity = entity,
                status = SyncControl.ActionStatus.COMPLETED,
                data = mapOf("id" to "1", "name" to "John"),
                actionedAt = datetime,
                eventId = "event-completed-1"
            ),
            SyncControl.Action(
                id = 0,
                action = SyncControl.ActionType.UPDATE,
                entity = entity,
                status = SyncControl.ActionStatus.COMPLETED,
                data = mapOf("id" to "2", "name" to "Alice"),
                actionedAt = datetime,
                eventId = "event-completed-2"
            )
        )

        // When
        controlManagerDatabaseHelper.addActionsCompleted(actions)

        // Then
        val completedActions = controlManagerDatabaseHelper.getCompletedActionsAfterDatetime(0)
        Assert.assertEquals(2, completedActions.size)
        Assert.assertEquals(setOf("event-completed-1", "event-completed-2"), completedActions.map { it.eventId }.toSet())
    }

    @Test
    fun addActionsCompletedRollbackWhenSecondActionFailsIsSuccess() {
        // Given
        val entity = "entity_transaction_123"
        val datetime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
        driver.registerEntity(entity)

        val actions = listOf(
            SyncControl.Action(
                id = 0,
                action = SyncControl.ActionType.INSERT,
                entity = entity,
                status = SyncControl.ActionStatus.COMPLETED,
                data = mapOf("id" to "1", "name" to "John"),
                actionedAt = datetime,
                eventId = "event-transaction-1"
            ),
            SyncControl.Action(
                id = 0,
                action = SyncControl.ActionType.UPDATE,
                entity = entity,
                status = SyncControl.ActionStatus.COMPLETED,
                data = mapOf("id" to "2", "invalid" to Exception("invalid value")),
                actionedAt = datetime,
                eventId = "event-transaction-2"
            )
        )

        // When
        try {
            controlManagerDatabaseHelper.addActionsCompleted(actions)
            fail("Expected an exception due invalid data serialization")
        } catch (_: Exception) {
            // Then
            val insertedRows = driver.executeQuery(
                null,
                "SELECT COUNT(*) FROM ${QueueActionsTable.TABLE_NAME}",
                {
                    QueryResult.Value(it.getLong(0))
                },
                0
            ).value

            Assert.assertEquals(0L, insertedRows)
        }
    }

    @Test
    fun getPendingActionsIsSuccess() {
        // Given
        val entity = "entity123"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name"),
            Horus.Attribute("flag", true),
            Horus.Attribute("number", Random.nextInt()),
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT, flag BOOLEAN)")
        driver.registerEntity(entity)

        controlManagerDatabaseHelper.addActionInsert(entity, attributes)

        // When
        val pendingActions = controlManagerDatabaseHelper.getPendingActions()

        // Then
        Assert.assertEquals(1, pendingActions.size)
        Assert.assertEquals(entity, pendingActions.first().entity)
        Assert.assertEquals(SyncControl.ActionType.INSERT, pendingActions.first().action)
        Assert.assertEquals(
            attributes.associate { it.name to it.value },
            pendingActions.first().data
        )
        Assert.assertEquals(SyncControl.ActionStatus.PENDING, pendingActions.first().status)
    }

    @Test
    fun completeActionsIsSuccess() {
        // Given
        val entity = "e9827733"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name"),
            Horus.Attribute("long", Random.nextLong())
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT, long INTEGER)")
        driver.registerEntity(entity)

        controlManagerDatabaseHelper.addActionInsert(entity, attributes)
        val pendingActions = controlManagerDatabaseHelper.getPendingActions()

        // When
        controlManagerDatabaseHelper.completeActions(pendingActions.map { it.id })

        // Then
        val result = driver.executeQuery(
            null,
            "SELECT ${QueueActionsTable.ATTR_STATUS} FROM ${QueueActionsTable.TABLE_NAME} WHERE id = ${pendingActions.first().id}",
            {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value

        Assert.assertEquals(SyncControl.ActionStatus.COMPLETED.id, result?.toInt())
    }

    @Test
    fun getLastActionCompleted() {
        // Given
        val entity = "e9827733"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name"),
            Horus.Attribute("long", Random.nextLong())
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT, long INTEGER)")
        driver.registerEntity(entity)

        controlManagerDatabaseHelper.addActionInsert(entity, attributes)
        controlManagerDatabaseHelper.addActionInsert(entity, attributes)

        val pendingActions = controlManagerDatabaseHelper.getPendingActions()
        controlManagerDatabaseHelper.completeActions(pendingActions.map { it.id })

        // When
        val lastActionCompleted = controlManagerDatabaseHelper.getLastActionCompleted()

        // Then
        Assert.assertEquals(pendingActions.last().id, lastActionCompleted?.id)
    }

    @Test
    fun getCompletedActionsAfterDatetime() {
        // Given
        val entity = "e9827733"
        val attributes = listOf(
            Horus.Attribute("id", "1"),
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
        driver.registerEntity(entity)

        controlManagerDatabaseHelper.addActionInsert(entity, attributes)

        val pendingActions = controlManagerDatabaseHelper.getPendingActions()
        controlManagerDatabaseHelper.completeActions(pendingActions.map { it.id })

        // When
        val completedActions = controlManagerDatabaseHelper.getCompletedActionsAfterDatetime(0)

        // Then
        Assert.assertEquals(1, completedActions.size)
    }

    @Test
    fun validateIsEntitiesIsWritable() {

        val entityWritable = "entity_writable_123"
        val entityReadOnly = "entity_read_only_423"

        driver.createTable(entityWritable, mapOf("id" to "TEXT"))
        driver.createTable(entityReadOnly, mapOf("id" to "TEXT"))

        driver.registerEntity(entityWritable)
        driver.registerEntity(entityReadOnly, false)

        // When
        val isWritable = controlManagerDatabaseHelper.isEntityCanBeWritable(entityWritable)
        val isReadOnly = !controlManagerDatabaseHelper.isEntityCanBeWritable(entityReadOnly)

        // Then
        assert(isWritable)
        assert(isReadOnly)
    }

    @Test
    fun getWritableEntitiesIsSuccess() {
        // Given
        val entityWritable = "entity_writable_123"
        val entityReadOnly = "entity_read_only_423"

        driver.createTable(entityWritable, mapOf("id" to "TEXT"))
        driver.createTable(entityReadOnly, mapOf("id" to "TEXT"))

        driver.registerEntity(entityWritable)
        driver.registerEntity(entityReadOnly, false)

        // When
        val writableEntities = controlManagerDatabaseHelper.getWritableEntityNames()

        // Then
        Assert.assertEquals(1, writableEntities.size)
        assert(writableEntities.contains(entityWritable))
        assert(!writableEntities.contains(entityReadOnly))
    }

    @Test
    fun getEntitiesWithFileReferencesIsSuccess() {
        // Given
        val anyEntity = "entity_123"
        val entityWithFileReferences = "entity_with_file_references_123"

        driver.createTable(anyEntity, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.createTable(entityWithFileReferences, mapOf("id" to "TEXT", "image" to "TEXT"))

        driver.registerEntity(anyEntity)
        driver.registerEntityAttribute(anyEntity, "id", AttributeType.Text)
        driver.registerEntityAttribute(anyEntity, "name", AttributeType.Text)

        driver.registerEntity(entityWithFileReferences)
        driver.registerEntityAttribute(entityWithFileReferences, "id", AttributeType.Text)
        driver.registerEntityAttribute(entityWithFileReferences, "image", AttributeType.RefFile)

        // When
        val entitiesWithFileReferences =
            controlManagerDatabaseHelper.getEntitiesWithAttributeType(AttributeType.RefFile)

        // Then
        Assert.assertEquals(1, entitiesWithFileReferences.size)
        assert(entitiesWithFileReferences.contains(entityWithFileReferences))
    }

    @Test
    fun getEntityAttributesWithTypeIsSuccess() {
        // Given
        val anyEntity = "entity_123"

        driver.createTable(anyEntity, mapOf("id" to "TEXT", "name" to "TEXT", "age" to "INTEGER"))

        driver.registerEntity(anyEntity)
        driver.registerEntityAttribute(anyEntity, "id", AttributeType.Text)
        driver.registerEntityAttribute(anyEntity, "name", AttributeType.Text)
        driver.registerEntityAttribute(anyEntity, "age", AttributeType.Integer)

        // When
        val attributesInteger = controlManagerDatabaseHelper.getEntityAttributesWithType(
            anyEntity,
            AttributeType.Integer
        )

        // Then
        Assert.assertEquals(1, attributesInteger.size)
        Assert.assertEquals("age", attributesInteger.first())
    }

    @Test
    fun clearDatabaseIsSuccess() {
        // Given
        val anyEntity = "entity_123"
        val entityWithFileReferences = "entity_with_file_references_123"

        driver.createTable(anyEntity, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.createTable(entityWithFileReferences, mapOf("id" to "TEXT", "image" to "TEXT"))

        val countTables = controlManagerDatabaseHelper.getTables().size
        Assert.assertTrue(countTables > 0)

        // When
        controlManagerDatabaseHelper.clearDatabase()

        // Then
        Assert.assertEquals(0, controlManagerDatabaseHelper.getTables().size)
    }

    @Test
    fun validateGetReadableEntitiesIsSuccess() {
        // Given
        val entityWritable = "entity_writable_123"
        val entityReadOnly = "entity_read_only_423"

        driver.createTable(entityWritable, mapOf("id" to "TEXT"))
        driver.createTable(entityReadOnly, mapOf("id" to "TEXT"))

        driver.registerEntity(entityWritable)
        driver.registerEntity(entityReadOnly, false)

        // When
        val readableEntities = controlManagerDatabaseHelper.getReadableEntityNames()

        // Then
        Assert.assertEquals(1, readableEntities.size)
        assert(readableEntities.contains(entityReadOnly))
    }

    @Test
    fun validateGetEntityLevelIsSuccess() {
        val entityLevel0 = "entity_level0"
        val entityLevel1 = "entity_level1"

        driver.createTable(entityLevel0, mapOf("id" to "TEXT"))
        driver.createTable(entityLevel1, mapOf("id" to "TEXT"))

        driver.registerEntity(entityLevel0, level = 0)
        driver.registerEntity(entityLevel1, level = 1)

        // When
        val level0 = controlManagerDatabaseHelper.getEntityLevel(entityLevel0)
        val level1 = controlManagerDatabaseHelper.getEntityLevel(entityLevel1)

        // Then
        Assert.assertEquals(0, level0)
        Assert.assertEquals(1, level1)
    }


    @Test(expected = IllegalArgumentException::class)
    fun validateGetEntityLevelNotFound() {
        val entityNotFound = "entity_not_found"

        // When
        controlManagerDatabaseHelper.getEntityLevel(entityNotFound)
    }


    @Test
    fun `when getEntitiesRelated from entity child then return parent entities related`(): Unit =
        runBlocking {
            // Given
            val entityParent = "entity_parent"
            val entityChild = "entity_child"

            driver.execute("CREATE TABLE $entityParent (id TEXT PRIMARY KEY, name TEXT)")
            driver.execute("CREATE TABLE $entityChild (id TEXT PRIMARY KEY, parent_id TEXT, name TEXT, FOREIGN KEY(parent_id) REFERENCES $entityParent(id))")

            driver.registerEntity(entityParent, level = 0)
            driver.registerEntity(entityChild, level = 1)

            // When
            controlManagerDatabaseHelper.getEntitiesRelated(entityChild) // First call to load cache
            val relatedEntities = controlManagerDatabaseHelper.getEntitiesRelated(entityChild)


            // Then
            Assert.assertEquals(1, relatedEntities.size)
            Assert.assertEquals(entityParent, relatedEntities.first().entity)
            Assert.assertEquals("parent_id", relatedEntities.first().attributesLinked.first())
        }

    @Test
    fun `when insertActionSequences simple is success`(): Unit = runBlocking {
        // Given
        val sequences = generateArray(50) { Random.nextLong() }

        // When
        controlManagerDatabaseHelper.insertActionSequences(sequences)

        // Then
        val result = driver.executeQuery(
            null,
            "SELECT COUNT(${QueueActionsSequenceTable.ATTR_SEQUENCE}) FROM ${QueueActionsSequenceTable.TABLE_NAME} WHERE ${QueueActionsSequenceTable.ATTR_SEQUENCE} IS NOT NULL",
            {
                QueryResult.Value(it.getLong(0))
            },
            0
        ).value
        Assert.assertEquals(sequences.size.toLong(), result)
    }

    @Test
    fun `when insertActionSequences is too long is success`(): Unit = runBlocking {
        // Given
        val sequences = generateArray(50000) { Random.nextLong() }

        // When
        controlManagerDatabaseHelper.insertActionSequences(sequences)

        // Then
        val result = driver.executeQuery(
            null,
            "SELECT COUNT(${QueueActionsSequenceTable.ATTR_SEQUENCE}) FROM ${QueueActionsSequenceTable.TABLE_NAME} WHERE ${QueueActionsSequenceTable.ATTR_SEQUENCE} IS NOT NULL",
            {
                QueryResult.Value(it.getLong(0))
            },
            0
        ).value
        Assert.assertEquals(sequences.size.toLong(), result)
    }

    @Test
    fun `when getExistsActionSequences is success`(): Unit = runBlocking {
        // Given
        val sequences = generateArray(50) { Random.nextLong() }
        controlManagerDatabaseHelper.insertActionSequences(sequences)

        val sequencesToCheck = sequences.take(30) + generateArray(20) { Random.nextLong() }

        // When
        val existsSequences =
            controlManagerDatabaseHelper.getExistsActionSequences(sequencesToCheck)

        // Then
        Assert.assertEquals(30, existsSequences.size)
        sequences.take(30).forEach {
            Assert.assertTrue(existsSequences.contains(it))
        }
    }

    @Test
    fun `when getExistsActionEventIds is success`() {
        // Given
        val entity = "entity_event_id_exists"
        driver.createTable(entity, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.registerEntity(entity)

        val row1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "1", "name" to "A"),
            1000L,
            "event-id-1"
        )
        val row2 = queueActionMapToCreate(
            SyncControl.ActionType.UPDATE,
            entity,
            mapOf("id" to "2", "name" to "B"),
            1001L,
            "event-id-2"
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)

        // When
        val result = controlManagerDatabaseHelper.getExistsActionEventIds(
            listOf("event-id-1", "event-id-x", "event-id-2")
        )

        // Then
        Assert.assertEquals(3, result.size)
        Assert.assertEquals(true, result["event-id-1"])
        Assert.assertEquals(false, result["event-id-x"])
        Assert.assertEquals(true, result["event-id-2"])
    }

    @Test
    fun `when getExistsActionEventIds with many eventIds then validate using chunked`() {
        // Given
        val entity = "entity_event_id_chunked"
        driver.createTable(entity, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.registerEntity(entity)

        val existingEventIds = listOf("event-id-10", "event-id-999", "event-id-1500", "event-id-2005")

        existingEventIds.forEachIndexed { index, eventId ->
            driver.insertOrThrow(
                QueueActionsTable.TABLE_NAME,
                queueActionMapToCreate(
                    SyncControl.ActionType.INSERT,
                    entity,
                    mapOf("id" to (index + 1).toString(), "name" to "Item-$index"),
                    2000L + index,
                    eventId
                )
            )
        }

        val eventIdsToCheck = (1..2005).map { "event-id-$it" }

        // When
        val result = controlManagerDatabaseHelper.getExistsActionEventIds(eventIdsToCheck)

        // Then
        Assert.assertEquals(eventIdsToCheck.size, result.size)
        existingEventIds.forEach { Assert.assertEquals(true, result[it]) }
        Assert.assertFalse(result.containsKey("event-id-2006"))
        Assert.assertEquals(false, result["event-id-500"])
        Assert.assertEquals(false, result["event-id-1"])
    }

    @Test
    fun `when insert actions using same event id then throw exception`() {
        // Given
        val e1 = "products_q1"
        val e2 = "orders_q1"

        driver.execute("PRAGMA foreign_keys=ON")
        driver.createTable(e1, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.createTable(e2, mapOf("id" to "TEXT", "name" to "TEXT"))

        driver.registerEntity(e1)
        driver.registerEntity(e2)

        val timeZoneBogota = TimeZone.of("America/Bogota")
        val testDate = LocalDate(2026, 5, 22)
        val epoch = testDate.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds
        val eventId = uuid()

        val row1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            e1,
            mapOf("id" to "1", "name" to "P1"),
            epoch,
            eventId
        )
        val row2 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            e2,
            mapOf("id" to "2", "name" to "O1"),
            epoch,
            eventId
        )

        try {
            driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
            driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)
            fail("Insertion should fail due to foreign key constraint")
        }catch (e: Exception){
            assert(true)
        }
    }


    @Test
    fun `when queryActions with timezone then filter by date correctly`(): Unit = runBlocking {
        // Given
        val e1 = "products_q1"
        val e2 = "orders_q1"

        driver.createTable(e1, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.createTable(e2, mapOf("id" to "TEXT", "name" to "TEXT"))

        driver.registerEntity(e1)
        driver.registerEntity(e2)

        val row1 = QueueActionsTable.mapToCreate(
            SyncControl.ActionType.INSERT,
            e1,
            mapOf("id" to "1", "name" to "P1")
        )
        val row2 = QueueActionsTable.mapToCreate(
            SyncControl.ActionType.INSERT,
            e2,
            mapOf("id" to "2", "name" to "O1")
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)

        // When: query using Bogotá timezone for today (2026-05-23)
        val timeZoneBogota = TimeZone.of("America/Bogota")
        val todayBogota = Clock.System.now().toLocalDateTime(timeZoneBogota).date

        val result = controlManagerDatabaseHelper.queryActions(
            listOf(e1),
            emptyMap(),
            todayBogota,
            null,
            timeZoneBogota
        )

        // Then: should find only e1 entity
        Assert.assertEquals(1, result.size)
        Assert.assertEquals(e1, result.first().entity)
    }

    @Test
    fun queryActions_filtersByEntityNames() {
        // Given
        val e1 = "products_q1"
        val e2 = "orders_q1"

        driver.createTable(e1, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.createTable(e2, mapOf("id" to "TEXT", "name" to "TEXT"))

        driver.registerEntity(e1)
        driver.registerEntity(e2)

        val timeZoneBogota = TimeZone.of("America/Bogota")
        val testDate = LocalDate(2026, 5, 22)
        val epoch = testDate.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds
        val eventIdRow2 = uuid()

        val row1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            e1,
            mapOf("id" to "1", "name" to "P1"),
            epoch
        )
        val row2 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            e2,
            mapOf("id" to "2", "name" to "O1"),
            epoch,
            eventIdRow2
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)

        // When: query only e1 entities
        val resultE1 = controlManagerDatabaseHelper.queryActions(
            listOf(e1),
            emptyMap(),
            testDate,
            null,
            timeZoneBogota
        )

        val resultE2 = controlManagerDatabaseHelper.queryActions(
            listOf(e2),
            emptyMap(),
            testDate,
            null,
            timeZoneBogota
        )

        // Then: should find only e1 entity
        Assert.assertEquals(1, resultE1.size)
        Assert.assertEquals(e1, resultE1.first().entity)
        Assert.assertEquals("1", resultE1.first().data["id"])
        Assert.assertNull(resultE1.first().eventId)

        // Then: should find only e2 entity
        Assert.assertEquals(1, resultE2.size)
        Assert.assertEquals(e2, resultE2.first().entity)
        Assert.assertEquals("2", resultE2.first().data["id"])
        Assert.assertNotNull(resultE2.first().eventId)
        Assert.assertEquals(eventIdRow2, resultE2.first().eventId)
    }

    @Test
    fun queryActions_filtersByDateRange() {
        // Given
        val entity = "entity_dates_q"
        driver.createTable(entity, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.registerEntity(entity)

        val timeZoneBogota = TimeZone.of("America/Bogota")

        // Create actions on different dates

        listOf(
            LocalDate(2026, 5, 20),
            LocalDate(2026, 5, 21),
            LocalDate(2026, 5, 22),
            LocalDate(2026, 5, 23)
        ).forEach {
            val epoch = it.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds
            val row = queueActionMapToCreate(
                SyncControl.ActionType.INSERT,
                entity,
                mapOf("id" to "1", "name" to "Before"),
                epoch
            )
            driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row)
        }

        // When: query between 2026-05-21 and 2026-05-23 (inclusive)
        val minDate = LocalDate(2026, 5, 21)
        val maxDate = LocalDate(2026, 5, 23)

        val result = controlManagerDatabaseHelper.queryActions(
            listOf(entity),
            emptyMap(),
            minDate,
            maxDate,
            timeZoneBogota
        )


        Assert.assertEquals(3, result.size)
    }

    @Test
    fun queryActions_filtersByMinDateOnly() {
        // Given
        val entity = "entity_min_date"
        driver.createTable(entity, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.registerEntity(entity)

        val timeZoneBogota = TimeZone.of("America/Bogota")

        val date1 = LocalDate(2026, 5, 20)
        val date2 = LocalDate(2026, 5, 22)

        val epoch1 = date1.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds
        val epoch2 = date2.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds

        val row1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "1", "name" to "Before"),
            epoch1
        )
        val row2 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "2", "name" to "After"),
            epoch2
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)

        val minDate = LocalDate(2026, 5, 21)

        val result = controlManagerDatabaseHelper.queryActions(
            listOf(entity),
            emptyMap(),
            minDate,
            null,
            timeZoneBogota
        )

        // Then: should find only row2 (2026-05-22) since it's after 2026-05-21
        Assert.assertEquals(0, result.size)
    }

    @Test
    fun queryActions_filtersByDataFilter_stringValue() {
        // Given
        val entity = "entity_filter_string"
        driver.createTable(entity, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.registerEntity(entity)

        val timeZoneBogota = TimeZone.of("America/Bogota")
        val testDate = LocalDate(2026, 5, 22)
        val epoch = testDate.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds

        val row1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "1", "name" to "John"),
            epoch
        )
        val row2 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "2", "name" to "Alice"),
            epoch
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)

        // When: filter by name = "John"
        val result = controlManagerDatabaseHelper.queryActions(
            listOf(entity),
            mapOf("name" to "John"),
            testDate,
            null,
            timeZoneBogota
        )

        // Then: should find only row1
        Assert.assertEquals(1, result.size)
        Assert.assertEquals("John", result.first().data["name"])
        Assert.assertEquals("1", result.first().data["id"])
    }

    @Test
    fun queryActions_filtersByDataFilter_booleanValue() {
        // Given
        val entity = "entity_filter_boolean"
        driver.createTable(entity, mapOf("id" to "TEXT", "active" to "BOOLEAN"))
        driver.registerEntity(entity)

        val timeZoneBogota = TimeZone.of("America/Bogota")
        val testDate = LocalDate(2026, 5, 22)
        val epoch = testDate.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds

        val rowTrue = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "1", "active" to true),
            epoch
        )
        val rowFalse = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "2", "active" to false),
            epoch
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, rowTrue)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, rowFalse)

        // When: filter by active = true
        val resultTrue = controlManagerDatabaseHelper.queryActions(
            listOf(entity),
            mapOf("active" to true),
            testDate,
            null,
            timeZoneBogota
        )

        // Then: should find only row with active = true
        Assert.assertEquals(1, resultTrue.size)
        Assert.assertEquals(true, resultTrue.first().data["active"])
        Assert.assertEquals("1", resultTrue.first().data["id"])

        // When: filter by active = false
        val resultFalse = controlManagerDatabaseHelper.queryActions(
            listOf(entity),
            mapOf("active" to false),
            testDate,
            null,
            timeZoneBogota
        )

        // Then: should find only row with active = false
        Assert.assertEquals(1, resultFalse.size)
        Assert.assertEquals(false, resultFalse.first().data["active"])
        Assert.assertEquals("2", resultFalse.first().data["id"])
    }

    @Test
    fun queryActions_filtersByMultipleDataFilters() {
        // Given
        val entity = "entity_multi_filter"
        driver.createTable(entity, mapOf("id" to "TEXT", "name" to "TEXT", "active" to "BOOLEAN"))
        driver.registerEntity(entity)

        val timeZoneBogota = TimeZone.of("America/Bogota")
        val testDate = LocalDate(2026, 5, 22)
        val epoch = testDate.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds

        val row1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "1", "name" to "John", "active" to true),
            epoch
        )
        val row2 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "2", "name" to "John", "active" to false),
            epoch
        )
        val row3 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            entity,
            mapOf("id" to "3", "name" to "Alice", "active" to true),
            epoch
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row1)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row2)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, row3)

        // When: filter by name = "John" AND active = true
        val result = controlManagerDatabaseHelper.queryActions(
            listOf(entity),
            mapOf("name" to "John", "active" to true),
            testDate,
            null,
            timeZoneBogota
        )

        // Then: should find only row1
        Assert.assertEquals(1, result.size)
        Assert.assertEquals("John", result.first().data["name"])
        Assert.assertEquals(true, result.first().data["active"])
        Assert.assertEquals("1", result.first().data["id"])
    }

    @Test
    fun queryActions_filtersByMultipleEntitiesAndDateRange() {
        // Given
        val e1 = "products_multi"
        val e2 = "orders_multi"
        driver.createTable(e1, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.createTable(e2, mapOf("id" to "TEXT", "name" to "TEXT"))
        driver.registerEntity(e1)
        driver.registerEntity(e2)

        val timeZoneBogota = TimeZone.of("America/Bogota")

        val date1 = LocalDate(2026, 5, 20)
        val date2 = LocalDate(2026, 5, 22)

        val epoch1 = date1.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds
        val epoch2 = date2.atTime(12, 0).toInstant(timeZoneBogota).epochSeconds

        // Products on date1 and date2
        val p1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            e1,
            mapOf("id" to "p1", "name" to "Product1"),
            epoch1
        )
        val p2 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            e1,
            mapOf("id" to "p2", "name" to "Product2"),
            epoch2
        )

        // Orders on date2
        val o1 = queueActionMapToCreate(
            SyncControl.ActionType.INSERT,
            e2,
            mapOf("id" to "o1", "name" to "Order1"),
            epoch2
        )

        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, p1)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, p2)
        driver.insertOrThrow(QueueActionsTable.TABLE_NAME, o1)

        // When: query both entities from date2 only
        val result = controlManagerDatabaseHelper.queryActions(
            listOf(e1, e2),
            emptyMap(),
            date2,
            null,
            timeZoneBogota
        )

        // Then: should find p2 and o1 (both on date2)
        Assert.assertEquals(2, result.size)
        val ids = result.map { it.data["id"] as String }.sorted()
        Assert.assertEquals(listOf("o1", "p2"), ids)
    }


    private fun queueActionMapToCreate(
        actionType: SyncControl.ActionType,
        entity: String,
        jsonData: Map<String, Any?>,
        datetime: Long,
        eventId: String? = null
    ) = mapOf(
        ATTR_ACTION_TYPE to actionType.id,
        ATTR_ENTITY to entity,
        ATTR_DATA to AnySerializer.decoderJSON.encodeToString(jsonData),
        ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
        ATTR_DATETIME to datetime,
        ATTR_EVENT_ID to eventId
    )

}
