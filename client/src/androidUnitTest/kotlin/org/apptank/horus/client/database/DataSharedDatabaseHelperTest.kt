package org.apptank.horus.client.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.base.decodeToMapAttributes
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.scheme.DataSharedTable
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.extensions.getRequireInt
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for the DataSharedDatabaseHelper class.
 * Tests the methods insert, truncate, and queryRecords.
 */
class DataSharedDatabaseHelperTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var databaseHelper: DataSharedDatabaseHelper

    /**
     * Sets up the test environment before each test.
     * Creates a new in-memory database and initializes the DataSharedDatabaseHelper.
     */
    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        databaseHelper = DataSharedDatabaseHelper("test_database", driver)

        // Create the data shared table
        driver.execute(DataSharedTable.SQL_CREATE_TABLE)
    }

    /**
     * Tests inserting a single entity record.
     * Verifies that the record is correctly inserted into the database.
     */
    @Test
    fun testInsertSingleRecord() {
        // Given
        val entityId = uuid()
        val entityName = "test_entity"
        val data = mapOf("key" to "value", "number" to 42)
        val entityShared = SyncControl.EntityShared(entityId, entityName, data)

        // When
        databaseHelper.insert(entityShared)

        // Then
        val count = getRecordCount()
        Assert.assertEquals(1, count)

        val record = getRecordById(entityId)
        Assert.assertNotNull(record)
        Assert.assertEquals(entityId, record!!["id"])
        Assert.assertEquals(entityName, record["entity"])
        val storedData = (record["data"] as String).decodeToMapAttributes()
        Assert.assertEquals(data["key"], storedData["key"])
        Assert.assertEquals(data["number"], storedData["number"])
    }

    /**
     * Tests inserting multiple entity records in a single call.
     * Verifies that all records are correctly inserted.
     */
    @Test
    fun testInsertMultipleRecords() {
        // Given
        val records = (1..5).map {
            val entityId = uuid()
            val entityName = "entity_$it"
            val data = mapOf(
                "id" to entityId,
                "name" to "name_$it",
                "value" to Random.nextInt(100)
            )
            SyncControl.EntityShared(entityId, entityName, data)
        }.toTypedArray()

        // When
        databaseHelper.insert(*records)

        // Then
        val count = getRecordCount()
        Assert.assertEquals(records.size, count)

        // Verify each record was inserted correctly
        records.forEach { expectedRecord ->
            val record = getRecordById(expectedRecord.entityId)
            Assert.assertNotNull(record)
            Assert.assertEquals(expectedRecord.entityId, record!!["id"])
            Assert.assertEquals(expectedRecord.entityName, record["entity"])
            val storedData = (record["data"] as String).decodeToMapAttributes()
            Assert.assertEquals(expectedRecord.data["name"], storedData["name"])
            Assert.assertEquals(expectedRecord.data["value"], storedData["value"])
        }
    }

    /**
     * Tests the truncate method.
     * Verifies that all records are removed after calling truncate.
     */
    @Test
    fun testTruncate() {
        // Given
        val records = (1..3).map {
            val entityId = uuid()
            val entityName = "entity_$it"
            val data = mapOf("key" to "value_$it")
            SyncControl.EntityShared(entityId, entityName, data)
        }.toTypedArray()

        databaseHelper.insert(*records)
        
        // Verify records were inserted
        val initialCount = getRecordCount()
        Assert.assertEquals(records.size, initialCount)

        // When
        databaseHelper.truncate()

        // Then
        val finalCount = getRecordCount()
        Assert.assertEquals(0, finalCount)
    }

    /**
     * Tests querying records with a basic query.
     * Verifies that all inserted records are returned.
     */
    @Test
    fun testQueryRecordsBasic() {
        // Given
        val records = (1..5).map {
            val entityId = uuid()
            val entityName = "test_entity"
            val data = mapOf("key" to "value_$it", "index" to it)
            SyncControl.EntityShared(entityId, entityName, data)
        }.toTypedArray()

        databaseHelper.insert(*records)

        // When
        val queryBuilder = SimpleQueryBuilder(DataSharedTable.TABLE_NAME)
        val results = databaseHelper.queryRecords(queryBuilder)

        // Then
        Assert.assertEquals(records.size, results.size)
    }

    /**
     * Tests querying records with order by clause.
     * Verifies that records are returned in the specified order.
     */
    @Test
    fun testQueryRecordsWithOrderBy() {
        // Given
        val records = mutableListOf<SyncControl.EntityShared>()
        for (i in 1..5) {
            val entityId = "id_$i" // Use predictable IDs for testing order
            val entityName = "test_entity"
            val data = mapOf("index" to i)
            records.add(SyncControl.EntityShared(entityId, entityName, data))
        }

        databaseHelper.insert(*records.toTypedArray())

        // When - query with ascending order by ID
        val queryBuilder = SimpleQueryBuilder(DataSharedTable.TABLE_NAME)
            .orderBy(DataSharedTable.ATTR_ID, SQL.OrderBy.ASC)

        val results = databaseHelper.queryRecords(queryBuilder)

        // Then
        Assert.assertEquals(records.size, results.size)

        // Check that the IDs are in ascending order
        for (i in 0 until results.size - 1) {
            val currentId = results[i][DataSharedTable.ATTR_ID] as String
            val nextId = results[i + 1][DataSharedTable.ATTR_ID] as String
            Assert.assertTrue(currentId < nextId)
        }
    }

    // Helper methods
    /**
     * Gets the total count of records in the data shared table.
     *
     * @return The number of records.
     */
    private fun getRecordCount(): Int {
        return driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM ${DataSharedTable.TABLE_NAME}",
            { QueryResult.Value(it.getRequireInt(0)) },
            0
        ).value
    }

    /**
     * Gets a record from the data shared table by ID.
     *
     * @param id The ID of the record to retrieve.
     * @return A map containing the record's data, or null if not found.
     */
    private fun getRecordById(id: String): Map<String, Any>? {
        var result: Map<String, Any>? = null
        
        driver.executeQuery(
            null,
            "SELECT * FROM ${DataSharedTable.TABLE_NAME} WHERE ${DataSharedTable.ATTR_ID} = '$id'",
            { cursor ->
                result = mapOf(
                    "id" to cursor.getString(0)!!,
                    "entity" to cursor.getString(1)!!,
                    "data" to cursor.getString(2)!!
                )
                QueryResult.Value(Unit)
            },
            0
        )
        
        return result
    }
} 