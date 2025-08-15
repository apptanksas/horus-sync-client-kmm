package org.apptank.horus.client.control

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.control.scheme.SyncControlTable
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.cache.MemoryCache
import org.apptank.horus.client.control.scheme.DataSharedTable
import org.apptank.horus.client.control.scheme.EntitiesTable
import org.apptank.horus.client.control.scheme.EntityAttributesTable
import org.apptank.horus.client.control.scheme.SyncFileTable
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.database.SyncControlDatabaseHelper
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.migration.domain.AttributeType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random


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
    fun `when getEntitiesRelated from entity child then return parent entities related`(): Unit = runBlocking {
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

}
