package com.apptank.horus.client.migration.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apptank.horus.client.DATA_MIGRATION_VERSION_1
import com.apptank.horus.client.DATA_MIGRATION_VERSION_2
import com.apptank.horus.client.DATA_MIGRATION_VERSION_3
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
        val tables = database.getTablesNames()

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
        val tables = database.getTablesNames()

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

        val tableFarmsColumnsV1 = database.getColumns("farms")
        val tableAnimalLotsColumnsV1 = database.getColumns("animals_lots")
        val tableLotsColumnsV1 = database.getColumns("lots")
        val tableFarmLocationsColumnsV1 = database.getColumns("farm_locations")

        // --> Migrate V2
        schema.migrate(driver, oldVersion, lastVersion, entitiesV2)

        // Then
        val tableFarmsColumnsV2 = database.getColumns("farms")
        val tableAnimalLotsColumnsV2 = database.getColumns("animals_lots")
        val tableLotsColumnsV2 = database.getColumns("lots")
        val tableFarmLocationsColumnsV2 = database.getColumns("farm_locations")


        Assert.assertEquals(1, oldVersion)
        Assert.assertEquals(2, lastVersion)

        // Validate that the column "name" was added in the version 2
        Assert.assertTrue(tableFarmsColumnsV1.isNotEmpty())
        Assert.assertNull(tableFarmsColumnsV1.find { it.name == "name" })
        Assert.assertTrue(tableFarmsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableFarmsColumnsV2.find { it.name == "name" })

        // Validate that the column "lot_id" was added in the version 2
        Assert.assertTrue(tableAnimalLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableAnimalLotsColumnsV1.find { it.name == "lot_id" })
        Assert.assertTrue(tableAnimalLotsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableAnimalLotsColumnsV2.find { it.name == "lot_id" })

        // Validate that the column "farm_" and "name" was added in the version 2
        Assert.assertTrue(tableLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "farm_id" })
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "name" })
        Assert.assertTrue(tableLotsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableLotsColumnsV2.find { it.name == "farm_id" })
        Assert.assertNotNull(tableLotsColumnsV2.find { it.name == "name" })

        // Validate that the column "longitude" was added in the version 2
        Assert.assertTrue(tableFarmLocationsColumnsV1.isNotEmpty())
        Assert.assertNull(tableFarmLocationsColumnsV1.find { it.name == "longitude" })
        Assert.assertTrue(tableFarmLocationsColumnsV2.isNotEmpty())
        Assert.assertNotNull(tableFarmLocationsColumnsV2.find { it.name == "longitude" })
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

        val tableFarmsColumnsV2 = database.getColumns("farms")
        val tableLotsColumnsV2 = database.getColumns("animals_lots")
        val tablesV2 = database.getTablesNames()

        // --> Migrate V3
        schema.migrate(driver, oldVersion, lastVersion, entitiesV3)

        // Then
        val tableFarmsColumnsV3 = database.getColumns("farms")
        val tableLotsColumnsV3 = database.getColumns("animals_lots")
        val tablesV3 = database.getTablesNames()


        Assert.assertEquals(2, oldVersion)
        Assert.assertEquals(3, lastVersion)

        // Validate that the column "destination" was added in the version 3
        Assert.assertTrue(tableFarmsColumnsV2.isNotEmpty())
        Assert.assertNull(tableFarmsColumnsV2.find { it.name == "destination" })
        Assert.assertTrue(tableFarmsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableFarmsColumnsV3.find { it.name == "destination" })


        // Validate that the column "animal_id" was added in the version 3
        Assert.assertTrue(tableLotsColumnsV2.isNotEmpty())
        Assert.assertNull(tableLotsColumnsV2.find { it.name == "animal_id" })
        Assert.assertTrue(tableLotsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableLotsColumnsV3.find { it.name == "animal_id" })

        // Validate new entity "farms_metadata" was added in the version 3
        Assert.assertTrue(tablesV2.notContains("farms_metadata"))
        Assert.assertTrue(tablesV3.contains("farms_metadata"))
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
        schema.create(driver,entitiesV1)

        val tableFarmsColumnsV1 = database.getColumns("farms")
        val tableLotsColumnsV1 = database.getColumns("lots")
        val tableAnimalLotsColumnsV1 = database.getColumns("animals_lots")
        val tablesV1 = database.getTablesNames()

        // --> Migrate V3
        schema.migrate(driver, oldVersion, lastVersion,entitiesV3)

        // Then
        val tableFarmsColumnsV3 = database.getColumns("farms")
        val tableAnimalLotsColumnsV3 = database.getColumns("animals_lots")
        val tableLotsColumnsV3 = database.getColumns("lots")
        val tablesV3 = database.getTablesNames()


        Assert.assertEquals(1, oldVersion)
        Assert.assertEquals(3, lastVersion)

        // Validate that the column "farm_" and "name" was added in the version 2
        Assert.assertTrue(tableLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "farm_id" })
        Assert.assertNull(tableLotsColumnsV1.find { it.name == "name" })
        Assert.assertTrue(tableLotsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableLotsColumnsV3.find { it.name == "farm_id" })
        Assert.assertNotNull(tableLotsColumnsV3.find { it.name == "name" })


        // Validate that the column "destination" was added in the version 3
        Assert.assertTrue(tableFarmsColumnsV1.isNotEmpty())
        Assert.assertNull(tableFarmsColumnsV1.find { it.name == "destination" })
        Assert.assertTrue(tableFarmsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableFarmsColumnsV3.find { it.name == "destination" })


        // Validate that the column "animal_id" was added in the version 3
        Assert.assertTrue(tableAnimalLotsColumnsV1.isNotEmpty())
        Assert.assertNull(tableAnimalLotsColumnsV1.find { it.name == "animal_id" })
        Assert.assertTrue(tableAnimalLotsColumnsV3.isNotEmpty())
        Assert.assertNotNull(tableAnimalLotsColumnsV3.find { it.name == "animal_id" })

        // Validate new entity "farms_metadata" was added in the version 3
        Assert.assertTrue(tablesV1.notContains("farms_metadata"))
        Assert.assertTrue(tablesV3.contains("farms_metadata"))
    }
}