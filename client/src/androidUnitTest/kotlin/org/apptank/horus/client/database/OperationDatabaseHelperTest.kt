package org.apptank.horus.client.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.cache.MemoryCache
import org.apptank.horus.client.control.scheme.EntityAttributesTable
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.builder.JoinableQueryBuilder
import org.apptank.horus.client.database.builder.QueryBuilder
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.builder.UnionQueryBuilder
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.extensions.getRequireInt
import org.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import org.apptank.horus.client.migration.domain.Attribute
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.migration.domain.EntityScheme
import org.apptank.horus.client.migration.domain.EntityType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.math.cos
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class OperationDatabaseHelperTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var databaseHelper: OperationDatabaseHelper

    private val entityName = "entity_name"

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        databaseHelper = OperationDatabaseHelper("database", driver)

        MemoryCache.flushCache()

        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "point" to "TEXT"
            )
        )
        driver.execute(EntityAttributesTable.SQL_CREATE_TABLE)
    }


    @Test
    fun executeOperationsInsertAndUpdateIsSuccess() {
        // Given
        val uuid = uuid()
        val nameExpected = "art2"
        val coordinates = Horus.Point.random()
        val actions = listOf(
            createInsertAction(uuid, "dog", coordinates),
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
        val pointResult = driver.executeQuery(
            null,
            "SELECT point FROM $entityName WHERE id = '$uuid'", {
                QueryResult.Value(it.getString(0))
            },
            0
        ).value

        Assert.assertEquals(nameExpected, nameResult)
        Assert.assertEquals(coordinates.toString(), pointResult)
    }

    @Test
    fun executeOperationsInsertAndDeleteIsSuccess() {
        // Given
        val uuid = uuid()
        val actions = listOf(
            createInsertAction(uuid, "dog"),
            createDeleteAction(uuid),
        )
        var postOperationValidation = false
        // When
        val result = databaseHelper.executeOperations(actions) {
            postOperationValidation = true
        }
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
        Assert.assertTrue(postOperationValidation)
    }

    @Test
    fun validateExecuteTransactionsIsSuccess() {
        // Given
        val uuid = uuid()
        val actions = listOf(
            createInsertAction(uuid, "dog"),
            createDeleteAction(uuid()),
        )
        var postOperationValidation = false
        // When
        val result = databaseHelper.executeOperations(actions) {
            postOperationValidation = true
        }
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
        Assert.assertFalse(postOperationValidation)
    }


    @Test
    fun validateExecuteTransactionsIsSuccessWhenIgnoreIfFailure() {
        // Given
        val uuid = uuid()

        driver.execute("PRAGMA foreign_keys=ON")

        driver.createTable(
            "owners",
            mapOf(
                "id" to "TEXT PRIMARY KEY",
                "name" to "TEXT NOT NULL"
            )
        )

        driver.createTable(
            "animals",
            mapOf(
                "id" to "TEXT PRIMARY KEY",
                "owner_id" to "TEXT NOT NULL",
                "name" to "TEXT",
                "species" to "TEXT",
                "FOREIGN KEY(owner_id)" to "REFERENCES owners(id) ON DELETE CASCADE"
            )
        )

        driver.insertOrThrow(
            "owners",
            mapOf(
                "id" to uuid,
                "name" to "owner1"
            )
        )

        val actions = listOf(
            DatabaseOperation.InsertRecord(
                "animals",
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("owner_id", uuid()),
                    SQL.ColumnValue("name", "dog"),
                    SQL.ColumnValue("species", "canine")
                )
            )
        )
        var postOperationValidation = false
        // When
        val result = databaseHelper.executeOperations(actions, ignoreIsFailure = true) {
            postOperationValidation = true
        }
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM animals", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value

        // Then
        Assert.assertEquals(0, count)
        Assert.assertTrue(result)
        Assert.assertTrue(postOperationValidation)
    }

    @Test
    fun validateExecuteTransactionsIsFailureWhenIgnoreIfFailureIsDisabled() {
        // Given
        val uuid = uuid()

        driver.execute("PRAGMA foreign_keys=ON")

        driver.createTable(
            "owners",
            mapOf(
                "id" to "TEXT PRIMARY KEY",
                "name" to "TEXT NOT NULL"
            )
        )

        driver.createTable(
            "animals",
            mapOf(
                "id" to "TEXT PRIMARY KEY",
                "owner_id" to "TEXT NOT NULL",
                "name" to "TEXT",
                "species" to "TEXT",
                "FOREIGN KEY(owner_id)" to "REFERENCES owners(id) ON DELETE CASCADE"
            )
        )

        driver.insertOrThrow(
            "owners",
            mapOf(
                "id" to uuid,
                "name" to "owner1"
            )
        )

        val actions = listOf(
            DatabaseOperation.InsertRecord(
                "animals",
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("owner_id", uuid()),
                    SQL.ColumnValue("name", "dog"),
                    SQL.ColumnValue("species", "canine")
                )
            )
        )
        var postOperationValidation = false

        // When
        val result = databaseHelper.executeOperations(actions) {
            postOperationValidation = true
        }


        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM animals", {
                QueryResult.Value(it.getRequireInt(0))
            },
            0
        ).value

        // Then
        Assert.assertEquals(0, count)
        Assert.assertFalse(result)
        Assert.assertFalse(postOperationValidation)
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
        val listActions = generateRandomArray {
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
        val listActions = generateRandomArray {
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
        val uuid2 = uuid()
        val listActions = listOf(
            createInsertAction(uuid, "dog"),
            createInsertAction(uuid2, "cat"),
            createDeleteAction(uuid2),
            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid),
                    SQL.ColumnValue("name", "cat"),
                    SQL.ColumnValue("column_to_force_error", "any")
                )
            ),

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

    @Test
    fun validateQueryRecordIsSuccess() {
        // Given
        val entityName = "my_entity"
        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "value" to "INTEGER",
                "float" to "FLOAT",
                "boolean" to "BOOLEAN"
            )
        )

        val listActions = generateRandomArray {
            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("name", "dog"),
                    SQL.ColumnValue("value", Random.nextInt()),
                    SQL.ColumnValue("float", Random.nextFloat()),
                    SQL.ColumnValue("boolean", Random.nextBoolean())
                )
            )
        }
        databaseHelper.insertWithTransaction(listActions)
        // When
        val result = databaseHelper.queryRecords(SimpleQueryBuilder(entityName))

        // Then
        Assert.assertEquals(listActions.size, result.size)
        result.forEach {
            Assert.assertTrue(it.containsKey("id"))
            Assert.assertTrue(it.containsKey("name"))
            Assert.assertTrue(it.containsKey("value"))
            Assert.assertTrue(it.containsKey("float"))
            Assert.assertTrue(it.containsKey("boolean"))
        }
    }

    @Test
    fun validateQueryRecordIsSuccessFindItemsNearbyCoordinates() {
        // Given
        val entityName = "points"
        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "point" to "TEXT"
            )
        )
        val pointReference = Horus.Point.random()
        val distanceInKm = 2

        val earthRadiusKm = 6371.0
        val deltaLat = distanceInKm / earthRadiusKm * (180.0 / Math.PI)
        val deltaLng =
            distanceInKm / (earthRadiusKm * cos(Math.toRadians(pointReference.latitude))) * (180.0 / Math.PI)

        val listActions = generateRandomArray {

            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val distance = Random.nextDouble(0.0, 1.0)
            val adjustedDistance = Math.sqrt(distance)

            val randomLat =
                pointReference.latitude + (adjustedDistance * deltaLat * Math.cos(angle))
            val randomLng =
                pointReference.longitude + (adjustedDistance * deltaLng * Math.sin(angle))

            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue(
                        "point", Horus.Point(
                            randomLat,
                            randomLng
                        ).toString()
                    )
                )
            )
        }


        databaseHelper.insertWithTransaction(listActions)
        // Other points outside the distance (between 3km and 10km from reference point)
        databaseHelper.insertWithTransaction(generateArray(10) {

            val minDistanceKm = 3.0  // Minimum distance to ensure points are outside 2km range
            val maxDistanceKm = 10.0

            val earthRadiusKm = 6371.0

            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            // Generate distance between minDistanceKm and maxDistanceKm
            val actualDistanceKm =
                minDistanceKm + Random.nextDouble() * (maxDistanceKm - minDistanceKm)

            val deltaLat = actualDistanceKm / earthRadiusKm * (180.0 / Math.PI)
            val deltaLng =
                actualDistanceKm / (earthRadiusKm * cos(Math.toRadians(pointReference.latitude))) * (180.0 / Math.PI)

            val randomLat = pointReference.latitude + (deltaLat * Math.cos(angle))
            val randomLng = pointReference.longitude + (deltaLng * Math.sin(angle))

            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue(
                        "point", Horus.Point(
                            randomLat,
                            randomLng
                        ).toString()
                    )
                )
            )
        })

        // When
        val result = databaseHelper.queryRecords(
            SimpleQueryBuilder(entityName).withExtension(
                SQL.Coordinates.WithIn("point", pointReference, distanceInKm.toDouble())
            )
        )

        // Then
        Assert.assertEquals(listActions.size, result.size)
    }

    @Test
    fun validateQueryRecordWithSelectTwoIsSuccess() {
        // Given
        val entityName = "my_entity"
        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "value" to "INTEGER",
                "float" to "FLOAT",
                "boolean" to "BOOLEAN"
            )
        )

        val listActions = generateRandomArray {
            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("name", "dog 'Olin"),
                    SQL.ColumnValue("value", Random.nextInt()),
                    SQL.ColumnValue("float", Random.nextFloat()),
                    SQL.ColumnValue("boolean", Random.nextBoolean())
                )
            )
        }
        databaseHelper.insertWithTransaction(listActions)
        // When
        val result = databaseHelper.queryRecords(SimpleQueryBuilder(entityName).apply {
            select("name", "boolean")
        })

        // Then
        Assert.assertEquals(listActions.size, result.size)
        result.forEach {
            Assert.assertTrue(it.containsKey("name"))
            Assert.assertTrue(it.containsKey("boolean"))
        }
    }

    @Test
    fun validateCountRecordsIsSuccess() {
        // Given
        val entityName = "my_entity"
        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "value" to "INTEGER",
                "float" to "FLOAT",
                "boolean" to "BOOLEAN"
            )
        )

        val listActions = generateRandomArray {
            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("name", "dog 'Olin"),
                    SQL.ColumnValue("value", Random.nextInt()),
                    SQL.ColumnValue("float", Random.nextFloat()),
                    SQL.ColumnValue("boolean", Random.nextBoolean())
                )
            )
        }
        databaseHelper.insertWithTransaction(listActions)
        // When
        val result = databaseHelper.countRecords(SimpleQueryBuilder(entityName))

        // Then
        assertEquals(listActions.size, result)
    }


    @Test
    fun validateCountRecordsIsZero() {
        // Given
        val entityName = "my_entity"
        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "value" to "INTEGER",
                "float" to "FLOAT",
                "boolean" to "BOOLEAN"
            )
        )

        // When
        val result = databaseHelper.countRecords(SimpleQueryBuilder(entityName))

        // Then
        assertEquals(0, result)
    }

    @Test
    fun validateCountRecordsWithJoinableQueryBuilderIsSuccess() {
        // Given
        val table1 = "table1_join"
        val table2 = "table2_join"
        driver.createTable(table1, mapOf("id" to "INTEGER PRIMARY KEY", "name" to "TEXT"))
        driver.createTable(table2, mapOf("id" to "INTEGER PRIMARY KEY", "table1_id" to "INTEGER", "extra" to "TEXT"))

        databaseHelper.insertWithTransaction(listOf(
            DatabaseOperation.InsertRecord(table1, listOf(SQL.ColumnValue("id", 1), SQL.ColumnValue("name", "T1-1"))),
            DatabaseOperation.InsertRecord(table1, listOf(SQL.ColumnValue("id", 2), SQL.ColumnValue("name", "T1-2")))
        ))

        databaseHelper.insertWithTransaction(listOf(
            DatabaseOperation.InsertRecord(table2, listOf(SQL.ColumnValue("id", 1), SQL.ColumnValue("table1_id", 1), SQL.ColumnValue("extra", "E1"))),
            DatabaseOperation.InsertRecord(table2, listOf(SQL.ColumnValue("id", 2), SQL.ColumnValue("table1_id", 1), SQL.ColumnValue("extra", "E2"))),
            DatabaseOperation.InsertRecord(table2, listOf(SQL.ColumnValue("id", 3), SQL.ColumnValue("table1_id", 2), SQL.ColumnValue("extra", "E3")))
        ))

        // When
        val builder = JoinableQueryBuilder(table1)
            .innerJoin(table2, "$table1.id = $table2.table1_id")
            .where(SQL.WhereCondition(SQL.ColumnValue("$table1.id", 1)))

        val result = databaseHelper.countRecords(builder)

        // Then
        assertEquals(2, result)
    }

    @Test
    fun validateCountRecordsWithUnionQueryBuilderIsSuccess() {
        // Given
        val tableA = "tableA_union"
        val tableB = "tableB_union"
        driver.createTable(tableA, mapOf("id" to "INTEGER PRIMARY KEY", "val" to "TEXT"))
        driver.createTable(tableB, mapOf("id" to "INTEGER PRIMARY KEY", "val" to "TEXT"))

        databaseHelper.insertWithTransaction(listOf(
            DatabaseOperation.InsertRecord(tableA, listOf(SQL.ColumnValue("id", 1), SQL.ColumnValue("val", "A1"))),
            DatabaseOperation.InsertRecord(tableA, listOf(SQL.ColumnValue("id", 2), SQL.ColumnValue("val", "A2")))
        ))

        databaseHelper.insertWithTransaction(listOf(
            DatabaseOperation.InsertRecord(tableB, listOf(SQL.ColumnValue("id", 3), SQL.ColumnValue("val", "B1")))
        ))

        // When
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder(tableA))
            .add(SimpleQueryBuilder(tableB))

        val result = databaseHelper.countRecords(builder)

        // Then
        assertEquals(3, result)
    }

    @Test
    fun validateCountRecordsWithUnionQueryBuilderIsSuccessUsingExists() {
        // Given
        val tableA = "tableA_union"
        val tableB = "tableB_union"
        driver.createTable(tableA, mapOf("id" to "INTEGER PRIMARY KEY", "val" to "TEXT"))
        driver.createTable(tableB, mapOf("id" to "INTEGER PRIMARY KEY", "val" to "TEXT"))

        databaseHelper.insertWithTransaction(listOf(
            DatabaseOperation.InsertRecord(tableA, listOf(SQL.ColumnValue("id", 1), SQL.ColumnValue("val", "A1"))),
            DatabaseOperation.InsertRecord(tableA, listOf(SQL.ColumnValue("id", 2), SQL.ColumnValue("val", "A2")))
        ))

        databaseHelper.insertWithTransaction(listOf(
            DatabaseOperation.InsertRecord(tableB, listOf(SQL.ColumnValue("id", 3), SQL.ColumnValue("val", "B1")))
        ))

        // When
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder(tableA))
            .add(SimpleQueryBuilder(tableB))
            .unionAll()

        val result = databaseHelper.queryExists(builder)

        // Then
        assertTrue(result)
    }

    @Test
    fun validateCountRecordsWithUnionQueryBuilderIsSuccessUsingExistsThenEmpty() {
        // Given
        val tableA = "tableA_union"
        val tableB = "tableB_union"
        driver.createTable(tableA, mapOf("id" to "INTEGER PRIMARY KEY", "val" to "TEXT"))
        driver.createTable(tableB, mapOf("id" to "INTEGER PRIMARY KEY", "val" to "TEXT"))

        // When
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder(tableA))
            .add(SimpleQueryBuilder(tableB))
            .unionAll().asExists()

        val result = databaseHelper.queryExists(builder)

        // Then
        assertFalse(result)
    }

    @Test
    fun validateDeleteRecordsWithConstraintsEnabled() {
        // Given
        val parentEntityName = "parent_entity"
        val childEntityName = "child_entity"

        driver.createTable(
            parentEntityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT"
            )
        )

        driver.createTable(
            childEntityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "parent_id" to "STRING"
            ), listOf("FOREIGN KEY (parent_id) REFERENCES $parentEntityName(id)")
        )

        driver.execute("PRAGMA foreign_keys=ON")

        val parentId = uuid()
        val childId = uuid()
        val listActions = listOf(
            DatabaseOperation.InsertRecord(
                parentEntityName,
                listOf(
                    SQL.ColumnValue("id", parentId),
                    SQL.ColumnValue("name", "parent")
                )
            ),
            DatabaseOperation.InsertRecord(
                childEntityName,
                listOf(
                    SQL.ColumnValue("id", childId),
                    SQL.ColumnValue("name", "child"),
                    SQL.ColumnValue("parent_id", parentId)
                )
            )
        )
        databaseHelper.insertWithTransaction(listActions)

        // When
        try {

            databaseHelper.deleteRecords(
                parentEntityName,
                listOf(
                    SQL.WhereCondition(
                        SQL.ColumnValue("id", parentId)
                    )
                )
            )

            // Then
            Assert.fail()
        } catch (e: Throwable) {
            Assert.assertEquals(
                "[SQLITE_CONSTRAINT_FOREIGNKEY] A foreign key constraint failed (FOREIGN KEY constraint failed)",
                e.message
            )
        }

    }

    @Test
    fun validateDeleteRecordsWithConstraintsDisabled() {
        // Given
        val parentEntityName = "parent_entity"
        val childEntityName = "child_entity"

        driver.createTable(
            parentEntityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT"
            )
        )

        driver.createTable(
            childEntityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "parent_id" to "STRING"
            ), listOf("FOREIGN KEY (parent_id) REFERENCES $parentEntityName(id)")
        )

        driver.execute("PRAGMA foreign_keys=ON")

        val parentId = uuid()
        val childId = uuid()
        val listActions = listOf(
            DatabaseOperation.InsertRecord(
                parentEntityName,
                listOf(
                    SQL.ColumnValue("id", parentId),
                    SQL.ColumnValue("name", "parent")
                )
            ),
            DatabaseOperation.InsertRecord(
                childEntityName,
                listOf(
                    SQL.ColumnValue("id", childId),
                    SQL.ColumnValue("name", "child"),
                    SQL.ColumnValue("parent_id", parentId)
                )
            )
        )
        databaseHelper.insertWithTransaction(listActions)

        // When
        val result = databaseHelper.deleteRecords(
            parentEntityName,
            listOf(
                SQL.WhereCondition(
                    SQL.ColumnValue("id", parentId)
                )
            ),
            disableForeignKeys = true
        )

        // Then
        Assert.assertTrue(result.isSuccess)
    }

    @Test
    fun validateTruncate() {
        // Given
        val entityName = "my_entity"
        driver.createTable(
            entityName,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                "value" to "INTEGER",
                "float" to "FLOAT",
                "boolean" to "BOOLEAN"
            )
        )

        val listActions = generateRandomArray {
            DatabaseOperation.InsertRecord(
                entityName,
                listOf(
                    SQL.ColumnValue("id", uuid()),
                    SQL.ColumnValue("name", "dog 'Olin"),
                    SQL.ColumnValue("value", Random.nextInt()),
                    SQL.ColumnValue("float", Random.nextFloat()),
                    SQL.ColumnValue("boolean", Random.nextBoolean())
                )
            )
        }
        databaseHelper.insertWithTransaction(listActions)

        // When
        databaseHelper.truncate(entityName)

        // Then
        val count = getCountFromTable(entityName)
        Assert.assertEquals(0, count)
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

    private fun createInsertAction(uuid: String, name: String, point: Horus.Point? = null) =
        DatabaseOperation.InsertRecord(
            entityName,
            listOf(
                SQL.ColumnValue("id", uuid),
                SQL.ColumnValue("name", name),
                SQL.ColumnValue("point", point ?: Horus.Point.random())
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