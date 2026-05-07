package org.apptank.horus.client.migration.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.DATA_MIGRATION_VERSION_1
import org.apptank.horus.client.DATA_MIGRATION_VERSION_2
import org.apptank.horus.client.DATA_MIGRATION_VERSION_3
import org.apptank.horus.client.DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE
import org.apptank.horus.client.KotlinLogger
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.buildEntitiesSchemeFromJSON
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.extensions.notContains
import org.apptank.horus.client.control.scheme.EntitiesTable
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.Column
import org.apptank.horus.client.database.struct.Cursor
import org.apptank.horus.client.database.struct.CursorValue
import org.apptank.horus.client.migration.domain.getLastVersion
import org.apptank.horus.client.migration.network.toScheme
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class HorusDatabaseSchemaTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver

    private lateinit var schema: HorusDatabase.Schema
    private lateinit var database: HorusDatabase

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        schema = HorusDatabase.Schema
        database = HorusDatabase("databaseName", driver)

        HorusContainer.setupLogger(KotlinLogger())
    }

    @Test
    fun migrationIsSuccess() {
        // Given
        val entities = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        val countEntitiesExpected = 6
        val versionExpected = 1L

        // When
        schema.create(driver, entities)
        val lastVersion = entities.getLastVersion()

        // Then
        val tables = database.getTableEntities()
        val tableColumnsProducts = database.getColumns("products")

        Assert.assertEquals(countEntitiesExpected, tables.size)
        Assert.assertEquals(versionExpected, lastVersion)
        Assert.assertEquals(0, tables.find { it.name == "products" }?.level)
        Assert.assertEquals(1, tables.find { it.name == "lots" }?.level)
        Assert.assertEquals(2, tables.find { it.name == "categories_lots" }?.level)
        Assert.assertNotNull(tableColumnsProducts.find { it.name == "geo_point" })
    }


    @Test
    fun migrationWithVersionExistsSuccess() {
        // Given
        val entities = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        val countEntitiesExpected = 6
        // When
        schema.create(driver, entities)
        schema.create(driver)

        // Then
        val tables = database.getTableEntities()

        Assert.assertEquals(countEntitiesExpected, tables.size)
    }

    @Test
    fun migrateDatabaseV1ToV2() {
        // Given

        val entitiesV1 = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        val entitiesV2 = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_2).map { it.toScheme() }

        val oldVersion = entitiesV1.getLastVersion()
        val lastVersion = entitiesV2.getLastVersion()

        // When

        // --> Migrate V1
        schema.create(driver, entitiesV1)

        val tableProductsColumnsV1 = database.getColumns("products")
        val tableCategoryLotsColumnsV1 = database.getColumns("categories_lots")
        val tableLotsColumnsV1 = database.getColumns("lots")
        val tableProductLocationsColumnsV1 = database.getColumns("product_locations")

        // --> Migrate V2
        schema.migrate(driver, oldVersion, lastVersion, entitiesV2)

        // Then
        val tableProductsColumnsV2 = database.getColumns("products")
        val tableCategoryLotsColumnsV2 = database.getColumns("categories_lots")
        val tableLotsColumnsV2 = database.getColumns("lots")
        val tableProductLocationsColumnsV2 = database.getColumns("product_locations")


        Assert.assertEquals(1, oldVersion)
        Assert.assertEquals(2, lastVersion)

        // Validate that the column "name" was added in the version 2
        Assert.assertTrue(tableProductsColumnsV1.isNotEmpty())
        Assert.assertNull(tableProductsColumnsV1.find { it.name == "name" })
        Assert.assertTrue(tableProductsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableProductsColumnsV2.find { it.name == "name" })

        // Validate that the column "lot_id" was added in the version 2
        Assert.assertTrue(tableCategoryLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableCategoryLotsColumnsV1.find { it.name == "lot_id" })
        Assert.assertTrue(tableCategoryLotsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableCategoryLotsColumnsV2.find { it.name == "lot_id" })

        // Validate that the column "product_" and "name" was added in the version 2
        Assert.assertTrue(tableLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "product_id" })
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "name" })
        Assert.assertTrue(tableLotsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableLotsColumnsV2.find { it.name == "product_id" })
        Assert.assertNotNull(tableLotsColumnsV2.find { it.name == "name" })

        // Validate that the column "longitude" was added in the version 2
        Assert.assertTrue(tableProductLocationsColumnsV1.isNotEmpty())
        Assert.assertNull(tableProductLocationsColumnsV1.find { it.name == "longitude" })
        Assert.assertTrue(tableProductLocationsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableProductLocationsColumnsV2.find { it.name == "longitude" })
        Assert.assertNotNull(tableCategoryLotsColumnsV2.find { it.name == "sync_hash" })
        Assert.assertNotNull(tableCategoryLotsColumnsV2.find { it.name == "sync_owner_id" })
        Assert.assertNotNull(tableCategoryLotsColumnsV2.find { it.name == "sync_created_at" })
        Assert.assertNotNull(tableCategoryLotsColumnsV2.find { it.name == "sync_updated_at" })
    }


    @Test
    fun migrateDatabaseV2ToV3() {
        // Given
        val entitiesV2 = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_2).map { it.toScheme() }
        val entitiesV3 = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_3).map { it.toScheme() }

        val oldVersion = entitiesV2.getLastVersion()
        val lastVersion = entitiesV3.getLastVersion()

        // When

        // --> Migrate V2
        schema.create(driver, entitiesV2)

        val tableProductsColumnsV2 = database.getColumns("products")
        val tableLotsColumnsV2 = database.getColumns("categories_lots")
        val tablesV2 = database.getTableEntities().map { it.name }

        // --> Migrate V3
        schema.migrate(driver, oldVersion, lastVersion, entitiesV3)

        // Then
        val tableProductsColumnsV3 = database.getColumns("products")
        val tableLotsColumnsV3 = database.getColumns("categories_lots")
        val tablesV3 = database.getTableEntities().map { it.name }


        Assert.assertEquals(2, oldVersion)
        Assert.assertEquals(3, lastVersion)

        // Validate that the column "destination" was added in the version 3
        Assert.assertTrue(tableProductsColumnsV2.isNotEmpty())
        Assert.assertNull(tableProductsColumnsV2.find { it.name == "destination" })
        Assert.assertTrue(tableProductsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableProductsColumnsV3.find { it.name == "destination" })
        Assert.assertFalse(
            tableProductsColumnsV3.find { it.name == "destination" }?.nullable ?: true
        )


        // Validate that the column "product_id" was added in the version 3
        Assert.assertTrue(tableLotsColumnsV2.isNotEmpty())
        Assert.assertNull(tableLotsColumnsV2.find { it.name == "product_id" })
        Assert.assertTrue(tableLotsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableLotsColumnsV3.find { it.name == "product_id" })
        Assert.assertFalse(tableLotsColumnsV3.find { it.name == "product_id" }?.nullable ?: true)

        // Validate new entity "products_metadata" was added in the version 3
        Assert.assertTrue(tablesV2.notContains("products_metadata"))
        Assert.assertTrue(tablesV3.contains("products_metadata"))
    }

    @Test
    fun migrateDatabaseV1ToV3() {
        // Given
        val entitiesV1 = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        val entitiesV3 = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_3).map { it.toScheme() }

        val oldVersion = entitiesV1.getLastVersion()
        val lastVersion = entitiesV3.getLastVersion()

        // When

        // --> Migrate V1
        schema.create(driver, entitiesV1)

        val tableProductsColumnsV1 = database.getColumns("products")
        val tableLotsColumnsV1 = database.getColumns("lots")
        val tableCategoryLotsColumnsV1 = database.getColumns("categories_lots")
        val tablesV1 = database.getTableEntities().map { it.name }

        // --> Migrate V3
        schema.migrate(driver, oldVersion, lastVersion, entitiesV3)

        // Then
        val tableProductsColumnsV3 = database.getColumns("products")
        val tableCategoryLotsColumnsV3 = database.getColumns("categories_lots")
        val tableLotsColumnsV3 = database.getColumns("lots")
        val tablesV3 = database.getTableEntities().map { it.name }


        Assert.assertEquals(1, oldVersion)
        Assert.assertEquals(3, lastVersion)

        // Validate that the column "product_" and "name" was added in the version 2
        Assert.assertTrue(tableLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "product_id" })
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "name" })
        Assert.assertTrue(tableLotsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableLotsColumnsV3.find { it.name == "product_id" })
        Assert.assertNotNull(tableLotsColumnsV3.find { it.name == "name" })


        // Validate that the column "destination" was added in the version 3
        Assert.assertTrue(tableProductsColumnsV1.isNotEmpty())
        Assert.assertNull(tableProductsColumnsV1.find { it.name == "destination" })
        Assert.assertTrue(tableProductsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableProductsColumnsV3.find { it.name == "destination" })


        // Validate that the column "product_id" was added in the version 3
        Assert.assertTrue(tableCategoryLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableCategoryLotsColumnsV1.find { it.name == "product_id" })
        Assert.assertTrue(tableCategoryLotsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableCategoryLotsColumnsV3.find { it.name == "product_id" })

        // Validate new entity "products_metadata" was added in the version 3
        Assert.assertTrue(tablesV1.notContains("products_metadata"))
        Assert.assertTrue(tablesV3.contains("products_metadata"))
    }

    @Test
    fun migrateDatabaseV1ToV3ALT() {
        // Given
        val entitiesV1 = buildEntitiesSchemeFromJSON(getContentFromFileResource("migration_v1_alt.json")).map { it.toScheme() }
        val entitiesV3 = buildEntitiesSchemeFromJSON(getContentFromFileResource("migration_v3_alt.json")).map { it.toScheme() }

        val oldVersion = entitiesV1.getLastVersion()
        val lastVersion = entitiesV3.getLastVersion()

        // When

        // --> Migrate V1
        schema.create(driver, entitiesV1)

        val tablesV1 = database.getTableEntities().map { it.name }

        // --> Migrate V3
        schema.migrate(driver, oldVersion, lastVersion, entitiesV3)

        // Then
        val tablesV3 = database.getTableEntities().map { it.name }


        Assert.assertEquals(1, oldVersion)
        Assert.assertEquals(3, lastVersion)

        Assert.assertTrue(tablesV1.notContains("events_tasks_configuration"))
        Assert.assertTrue(tablesV1.notContains("events"))
        Assert.assertTrue(tablesV1.notContains("tasks"))
        Assert.assertTrue(tablesV1.notContains("events_metadata"))
        Assert.assertTrue(tablesV1.notContains("tasks_metadata"))
        Assert.assertTrue(tablesV3.contains("events_tasks_configuration"))
        Assert.assertTrue(tablesV3.contains("events"))
        Assert.assertTrue(tablesV3.contains("tasks"))
        Assert.assertTrue(tablesV3.contains("events_metadata"))
        Assert.assertTrue(tablesV3.contains("tasks_metadata"))
    }

    @Test
    fun validateEntitiesWritableAndReadable() {
        // Given
        val entities = buildEntitiesSchemeFromJSON(DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE)
            .map { it.toScheme() }

        // When
        schema.create(driver, entities)

        // Then
        val tableWritable = database.getTableEntities().find { it.name == "measures" }
        val tableReadable = database.getTableEntities().find { it.name == "product_breeds" }

        Assert.assertTrue(tableWritable?.isWritable == true)
        Assert.assertTrue(tableReadable?.isWritable == false)
    }

    /**
     * Regression test for the iOS ClassCastException:
     * "class kotlin.Long cannot be cast to class kotlin.Int"
     *
     * On Kotlin/Native (iOS), SQLite INTEGER columns are always returned as Long by the driver
     * (see SQLiteHelper.buildCursorValues: "INTEGER" -> CursorValue(cursor.getLong(...), column)).
     * Without the fix in Cursor.getValue, the raw Long flows through the unchecked generic cast.
     *
     * The result is captured as Any? to prevent JVM auto-conversion at the assignment site,
     * exposing the actual runtime class of the value returned by getValue<Int>():
     *   Without fix: runtime class is Long  → assertion FAILS
     *   With fix:    runtime class is Integer → assertion PASSES
     */
    @Test
    fun getTableEntitiesLevelIsReadAsIntWithoutClassCastException() {
        // Given - schema with hierarchical entities (level 0, 1, 2)
        val entities = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_1).map { it.toScheme() }
        schema.create(driver, entities)

        // Then - correct numeric values (sanity check)
        val tableEntities = database.getTableEntities()
        Assert.assertEquals(0, tableEntities.find { it.name == "products" }?.level)
        Assert.assertEquals(1, tableEntities.find { it.name == "lots" }?.level)
        Assert.assertEquals(2, tableEntities.find { it.name == "categories_lots" }?.level)

        // Critical: query again via queryResult + getValue<Int>(), capturing result as Any?
        // so JVM cannot auto-unbox/convert the Long before we inspect its class.
        //
        // Without fix: getValue<Int>() uses unchecked cast (value as T); T is erased to Any,
        //   so the Long passes through untouched. Captured as Any? → javaClass == Long → FAILS.
        // With fix (reified + coerceValue): Long is converted to Int before return.
        //   Captured as Any? → javaClass == Integer → PASSES.
        val rawLevelValues: List<Any?> = database.queryResult(
            SimpleQueryBuilder(EntitiesTable.TABLE_NAME).build()
        ) { cursor ->
            cursor.getValue<Int>(EntitiesTable.ATTR_LEVEL) as Any?
        }

        Assert.assertTrue("Expected at least one entity with a level", rawLevelValues.isNotEmpty())
        rawLevelValues.forEach { rawLevel ->
            Assert.assertEquals(
                "getValue<Int>() returned ${rawLevel?.javaClass?.simpleName} at runtime, " +
                        "expected Integer. Without fix this is Long, causing ClassCastException on iOS.",
                Int::class.javaObjectType,
                rawLevel?.javaClass
            )
        }
    }

    /**
     * Unit test that directly simulates the iOS ClassCastException scenario at the Cursor level.
     *
     * Without fix: Cursor.getValue uses `value as T` (unchecked generic cast).
     *   The Long stored in CursorValue passes through as T=Int (erased).
     *   Captured as Any? → javaClass == Long  → assertion FAILS
     *   On iOS: Kotlin/Native enforces the type strictly → throws ClassCastException
     *
     * With fix (reified + coerceValue): Long is explicitly converted to Int.
     *   Captured as Any? → javaClass == Integer → assertion PASSES
     */
    @Test
    fun cursorCoerceValueHandlesLongToIntConversion() {
        // Given - CursorValue holds Long (what SQLiteHelper.buildCursorValues stores for INTEGER)
        val column = Column(position = 0, name = "level", type = "INTEGER", nullable = false)
        val cursor = Cursor(
            index = 0,
            table = "horus_entities",
            values = listOf(CursorValue(2L, column))
        )

        // When - capture as Any? to prevent JVM unboxing/conversion from hiding the underlying type
        val rawResult: Any? = cursor.getValue<Int>("level") as Any?

        // Then
        Assert.assertEquals(
            "getValue<Int>() returned ${rawResult?.javaClass?.simpleName} at runtime, " +
                    "expected Integer. Without fix the unchecked cast returns Long, " +
                    "which causes ClassCastException on iOS (Kotlin/Native enforces generic types).",
            Int::class.javaObjectType,
            rawResult?.javaClass
        )
        Assert.assertEquals(2, rawResult)

        // Verify Double → Float coercion as well
        val colDouble = Column(position = 0, name = "score", type = "REAL", nullable = false)
        val cursorDouble = Cursor(index = 0, table = "test", values = listOf(CursorValue(3.14, colDouble)))
        val rawScore: Any? = cursorDouble.getValue<Float>("score") as Any?
        Assert.assertEquals(Float::class.javaObjectType, rawScore?.javaClass)
        Assert.assertEquals(3.14f, rawScore)
    }
}