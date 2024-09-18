package com.apptank.horus.client.migration.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_1
import com.apptank.horus.client.DATA_MIGRATION_VERSION_2
import com.apptank.horus.client.DATA_MIGRATION_VERSION_3
import com.apptank.horus.client.DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE
import com.apptank.horus.client.buildEntitiesSchemeFromJSON
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.extensions.notContains
import com.apptank.horus.client.migration.domain.getLastVersion
import com.apptank.horus.client.migration.network.toScheme
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class HorusDatabaseSchemaTest {

    private lateinit var driver: JdbcSqliteDriver

    private lateinit var schema: HorusDatabase.Schema
    private lateinit var database: HorusDatabase

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        schema = HorusDatabase.Schema
        database = HorusDatabase("databaseName", driver)
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

        Assert.assertEquals(countEntitiesExpected, tables.size)
        Assert.assertEquals(versionExpected, lastVersion)
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
        Assert.assertFalse(tableProductsColumnsV3.find { it.name == "destination" }?.nullable ?: true)


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
}