package com.apptank.horus.client.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.extensions.getRequireInt
import horus.HorusDatabase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random


class DatabaseOperationHelperTest : TestCase() {

    private lateinit var database: HorusDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var databaseHelper: OperationDatabaseHelper

    private val entityName = "entity_name"

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        database = HorusDatabase(driver)
        databaseHelper = OperationDatabaseHelper(database, "database", driver)

        SQLiteHelper.flushCache()

        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT"
            )
        )
    }


    @Test
    fun executeOperationsInsertAndUpdateIsSuccess() {
        // Given
        val uuid = uuid()
        val nameExpected = "art2"
        val actions = listOf(
            createInsertAction(uuid, "dog"),
            createUpdateAction(uuid, nameExpected),
        )
        // When
        val result = databaseHelper.executeOperations(actions)
        // Then
        assert(result)
        val nameResult = driver.executeQuery(
            null,
            "SELECT name FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value
        Assert.assertEquals(nameExpected, nameResult)
    }

    @Test
    fun executeOperationsInsertAndDeleteIsSuccess() {
        // Given
        val uuid = uuid()
        val actions = listOf(
            createInsertAction(uuid, "dog"),
            createDeleteAction(uuid),
        )
        // When
        val result = databaseHelper.executeOperations(actions)
        // Then
        assert(result)
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
        Assert.assertEquals(0, count)
    }

    @Test
    fun validateExecuteTransactionsIsSuccess() {
        // Given
        val uuid = uuid()
        val actions = listOf(
            createInsertAction(uuid, "dog"),
            createDeleteAction(uuid()),
        )
        // When
        val result = databaseHelper.executeOperations(actions)
        // Then
        Assert.assertFalse(result)

        // Validate do not exist the insert
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
        Assert.assertEquals(0, count)
    }

    @Test
    fun executeOperationsWithVarargsInsertAndUpdateIsSuccess() {
        // Given
        val uuid = uuid()
        val nameExpected = "art2"
        val insertAction = createInsertAction(uuid, "dog")
        val updateAction = createUpdateAction(uuid, nameExpected)

        // When
        val result = databaseHelper.executeOperations(insertAction, updateAction)
        // Then
        assert(result)
        val nameResult = driver.executeQuery(
            null,
            "SELECT name FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value
        Assert.assertEquals(nameExpected, nameResult)
    }

    @Test
    fun validateInsertTransaction() {
        // Given
        val listActions = generateArray {
            createInsertAction(uuid(), "dog")
        }
        var postValidate = false

        // When
        val result = databaseHelper.insertTransaction(listActions) {
            postValidate = true
        }

        // Then
        assert(result)
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $entityName", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
        Assert.assertEquals(listActions.size, count)
        assert(postValidate)
    }

    @Test
    fun validateUpdateTransaction() {
        // Given
        val uuid = uuid()
        val nameExpected = Random.nextInt().toString()
        val listActions = generateArray {
            createUpdateAction(uuid, nameExpected)
        }
        val insertAction = createInsertAction(uuid, "dog")
        var postValidate = false

        // When
        databaseHelper.executeOperations(insertAction)
        val result = databaseHelper.updateRecordTransaction(listActions) {
            postValidate = true
        }

        // Then
        assert(result)
        val nameResult = driver.executeQuery(
            null,
            "SELECT name FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value
        Assert.assertEquals(nameExpected, nameResult)
        assert(postValidate)
    }

    @Test
    fun validateDeleteTransaction() {
        // Given
        val uuid = uuid()
        val listActions = listOf(createDeleteAction(uuid))
        val insertAction = createInsertAction(uuid, "dog")
        var postValidate = false

        // When
        val resultInsert = databaseHelper.executeOperations(insertAction)
        val result = databaseHelper.deleteRecordTransaction(listActions) {
            postValidate = true
        }

        // Then
        assert(resultInsert)
        assert(result)
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
        Assert.assertEquals(0, count)
        assert(postValidate)
    }

    @Test
    fun validateDeleteRecord() {
        // Given
        val uuid = uuid()
        val insertAction = createInsertAction(uuid, "dog")

        // When
        val resultInsert = databaseHelper.executeOperations(insertAction)
        val result = databaseHelper.deleteRecord(entityName, listOf(SQL.WhereCondition(
            SQL.ColumnValue("id", uuid)
        )))

        // Then
        assert(resultInsert)
        assert(result.isSuccess)
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
        Assert.assertEquals(0, count)
    }


    private fun createInsertAction(uuid: String, name: String) = DatabaseOperation.InsertRecord(
        entityName,
        listOf(
            SQL.ColumnValue("id", uuid),
            SQL.ColumnValue("name", name)
        )
    )

    private fun createUpdateAction(uuid: String, name: String) = DatabaseOperation.UpdateRecord(
        entityName,
        listOf(
            SQL.ColumnValue("name", name)
        ),
        listOf(
            SQL.WhereCondition(
                SQL.ColumnValue("id", uuid)
            )
        ),
        SQL.LogicOperator.AND
    )

    private fun createDeleteAction(uuid: String) = DatabaseOperation.DeleteRecord(
        entityName,
        listOf(
            SQL.WhereCondition(
                SQL.ColumnValue("id", uuid)
            )
        ),
        SQL.LogicOperator.AND
    )

}