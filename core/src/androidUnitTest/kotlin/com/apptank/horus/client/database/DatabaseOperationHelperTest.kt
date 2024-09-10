package com.apptank.horus.client.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.extensions.execute
import com.apptank.horus.client.extensions.getRequireInt
import com.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import com.apptank.horus.client.migration.domain.Attribute
import com.apptank.horus.client.migration.domain.AttributeType
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.EntityType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random


class DatabaseOperationHelperTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var databaseHelper: OperationDatabaseHelper

    private val entityName = "entity_name"

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        databaseHelper = OperationDatabaseHelper("database", driver)

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
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $entityName", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
        Assert.assertEquals(1, count)
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
        val result = databaseHelper.insertWithTransaction(listActions) {
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
        val result = databaseHelper.updateWithTransaction(listActions) {
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
        val result = databaseHelper.deleteWithTransaction(listActions) {
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
        val result = databaseHelper.deleteRecords(
            entityName, listOf(
                SQL.WhereCondition(
                    SQL.ColumnValue("id", uuid)
                )
            )
        )

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

    @Test
    fun validateTransactionRollbackWhenFail() {
        // Given
        val uuid = uuid()
        val listActions = listOf(
            createInsertAction(uuid, "dog"),
            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid),
                    SQL.ColumnValue("name", "cat"),
                    SQL.ColumnValue("column_to_force_error", "any")
                )
            )
        )
        // When
        databaseHelper.executeOperations(listActions)

        // Then
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $entityName", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
        Assert.assertEquals(0, count)
    }

    @Test
    fun validateDeleteParentAndOnCascadeDeleteChildren() {

        val delegate = DatabaseTablesCreatorDelegate(
            listOf(
                EntityScheme(
                    "users",
                    EntityType.WRITABLE,
                    listOf(
                        Attribute("id", AttributeType.PrimaryKeyUUID, false, version = 1),
                        Attribute("name", AttributeType.String, false, version = 1)
                    ),
                    1,
                    listOf(
                        // Addresses
                        EntityScheme(
                            "addresses",
                            EntityType.WRITABLE,
                            listOf(
                                Attribute("id", AttributeType.PrimaryKeyUUID, false, version = 1),
                                Attribute("street", AttributeType.String, false, version = 1),
                                Attribute(
                                    "user_id",
                                    AttributeType.Text,
                                    false,
                                    version = 1,
                                    linkedEntity = "users"
                                )
                            ),
                            1,
                            listOf(
                                // Addresses Objects
                                EntityScheme(
                                    "addresses_objects",
                                    EntityType.WRITABLE,
                                    listOf(
                                        Attribute(
                                            "id",
                                            AttributeType.PrimaryKeyUUID,
                                            false,
                                            version = 1
                                        ),
                                        Attribute("name", AttributeType.String, false, version = 1),
                                        Attribute(
                                            "address_id",
                                            AttributeType.Text,
                                            false,
                                            version = 1,
                                            linkedEntity = "addresses"
                                        )
                                    ),
                                    1,
                                    emptyList()
                                )
                            )
                        ),
                        // Phones
                        EntityScheme(
                            "phones",
                            EntityType.WRITABLE,
                            listOf(
                                Attribute("id", AttributeType.PrimaryKeyUUID, false, version = 1),
                                Attribute("number", AttributeType.String, false, version = 1),
                                Attribute(
                                    "user_id",
                                    AttributeType.Text,
                                    false,
                                    version = 1,
                                    linkedEntity = "users"
                                )
                            ),
                            1,
                            emptyList()
                        )
                    )
                )
            )
        )

        delegate.createTables {
            driver.execute(it)
        }
        driver.execute("PRAGMA foreign_keys=ON")

        val userId = uuid()
        val addressId = uuid()

        val inserts = listOf(
            DatabaseOperation.InsertRecord(
                "users",
                listOf(
                    SQL.ColumnValue("id", userId),
                    SQL.ColumnValue("name", "user1")
                )
            ),
            DatabaseOperation.InsertRecord(
                "addresses",
                listOf(
                    SQL.ColumnValue("id", addressId),
                    SQL.ColumnValue("street", "street1"),
                    SQL.ColumnValue("user_id", userId)
                )
            ),
            DatabaseOperation.InsertRecord(
                "addresses_objects",
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("name", "object1"),
                    SQL.ColumnValue("address_id", addressId)
                )
            ),
            DatabaseOperation.InsertRecord(
                "phones",
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("number", "123456789"),
                    SQL.ColumnValue("user_id", userId)
                )
            )
        )

        databaseHelper.executeOperations(inserts)

        // Validate inserts
        Assert.assertEquals(1, getCountFromTable("users"))
        Assert.assertEquals(1, getCountFromTable("addresses"))
        Assert.assertEquals(1, getCountFromTable("addresses_objects"))
        Assert.assertEquals(1, getCountFromTable("phones"))

        // Delete user
        val result = databaseHelper.deleteRecords(
            "users",
            listOf(
                SQL.WhereCondition(
                    SQL.ColumnValue("id", userId)
                )
            )
        )

        // Validate delete
        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(0, getCountFromTable("users"))
        Assert.assertEquals(0, getCountFromTable("addresses"))
        Assert.assertEquals(0, getCountFromTable("addresses_objects"))
        Assert.assertEquals(0, getCountFromTable("phones"))
    }

    private fun getCountFromTable(table: String): Int {
        return driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM $table", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value
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