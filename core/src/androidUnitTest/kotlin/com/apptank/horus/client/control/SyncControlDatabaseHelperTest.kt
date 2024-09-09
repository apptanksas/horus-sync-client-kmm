package com.apptank.horus.client.control

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.database.SQLiteHelper
import com.apptank.horus.client.extensions.execute
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class SyncControlDatabaseHelperTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var controlManagerDatabaseHelper: SyncControlDatabaseHelper

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        controlManagerDatabaseHelper = SyncControlDatabaseHelper("database", driver)

        HorusDatabase.Schema.create(driver)
        SQLiteHelper.flushCache()
    }


    @Test
    fun onCreateIsSuccess() {
        // Then
        val tablesNames = controlManagerDatabaseHelper.getTablesNames()

        Assert.assertTrue(tablesNames.contains(SyncControlTable.TABLE_NAME))
        Assert.assertTrue(tablesNames.contains(QueueActionsTable.TABLE_NAME))
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
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
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
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
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
            Horus.Attribute("name", "name")
        )
        driver.execute("CREATE TABLE $entity (id TEXT, name TEXT)")
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
        controlManagerDatabaseHelper.addActionInsert(entity, attributes)

        val pendingActions = controlManagerDatabaseHelper.getPendingActions()
        controlManagerDatabaseHelper.completeActions(pendingActions.map { it.id })

        // When
        val completedActions = controlManagerDatabaseHelper.getCompletedActionsAfterDatetime(0)

        // Then
        Assert.assertEquals(1, completedActions.size)
    }
}