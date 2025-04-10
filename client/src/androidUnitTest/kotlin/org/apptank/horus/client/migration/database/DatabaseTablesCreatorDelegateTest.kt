package org.apptank.horus.client.migration.database

import org.apptank.horus.client.migration.domain.Attribute
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.migration.domain.EntityScheme
import org.apptank.horus.client.migration.domain.EntityType
import org.junit.Assert
import org.junit.Test


class DatabaseTablesCreatorDelegateTest {

    @Test
    fun createSimpleTable() {
        // Given
        val tableName = "users"
        val delegate = DatabaseTablesCreatorDelegate(
            listOf(
                EntityScheme(
                    tableName,
                    EntityType.WRITABLE,
                    listOf(
                        Attribute("id", AttributeType.PrimaryKeyUUID, false, version = 1),
                        Attribute("id2", AttributeType.PrimaryKeyString, false, version = 1),
                        Attribute("id3", AttributeType.PrimaryKeyInteger, false, version = 1),
                        Attribute("age", AttributeType.Integer, false, version = 1),
                        Attribute("price", AttributeType.Float, false, version = 1),
                        Attribute("active", AttributeType.Boolean, true, version = 1),
                        Attribute("name", AttributeType.String, false, version = 1),
                        Attribute("description", AttributeType.Text, false, version = 1),
                        Attribute("json", AttributeType.Json, false, version = 1),
                        Attribute(
                            "type",
                            AttributeType.Enum,
                            false,
                            version = 1,
                            listOf("A", "B", "C")
                        ),
                        Attribute("timestamp", AttributeType.Timestamp, false, version = 1),
                        Attribute("uuid", AttributeType.UUID, false, version = 1),
                        Attribute("refFile", AttributeType.RefFile, false, version = 1)
                    ),
                    1,
                    emptyList()
                )
            )
        )
        val sqlExpected = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY NOT NULL,
                id2 TEXT PRIMARY KEY NOT NULL,
                id3 INTEGER PRIMARY KEY NOT NULL,
                age INTEGER NOT NULL,
                price REAL NOT NULL,
                active BOOLEAN,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                json TEXT NOT NULL,
                type TEXT CHECK (type IN ('A', 'B', 'C')) NOT NULL,
                timestamp TEXT NOT NULL,
                uuid TEXT NOT NULL,
                refFile TEXT NOT NULL
            )
        """.normalizeSQL()

        // When
        delegate.createTables { sql: String ->
            // Then
            Assert.assertEquals(sqlExpected, sql)
        }
    }

    @Test
    fun createTableWithRelations() {
        // Given
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
        val sqlExpected = listOf(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS phones (
                id TEXT PRIMARY KEY NOT NULL,
                number TEXT NOT NULL,
                user_id TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS addresses (
                id TEXT PRIMARY KEY NOT NULL,
                street TEXT NOT NULL,
                user_id TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS addresses_objects (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                address_id TEXT NOT NULL,
                FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE CASCADE
            )
            """
        )

        // When
        var index = 0
        delegate.createTables { sql: String ->
            val expected = sqlExpected[index].normalizeSQL()
            // Then
            Assert.assertEquals(expected, sql)
            index++
        }
    }

    @Test
    fun createTableWithRelationsWithNotDeleteCascade() {
        // Given
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
                                    linkedEntity = "users",
                                    deleteOnCascade = false
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
                                            linkedEntity = "addresses",
                                            deleteOnCascade = false
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
                                    linkedEntity = "users",
                                    deleteOnCascade = false
                                )
                            ),
                            1,
                            emptyList()
                        )
                    )
                )
            )
        )
        val sqlExpected = listOf(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS phones (
                id TEXT PRIMARY KEY NOT NULL,
                number TEXT NOT NULL,
                user_id TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS addresses (
                id TEXT PRIMARY KEY NOT NULL,
                street TEXT NOT NULL,
                user_id TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS addresses_objects (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                address_id TEXT NOT NULL,
                FOREIGN KEY (address_id) REFERENCES addresses(id)
            )
            """
        )

        // When
        var index = 0
        delegate.createTables { sql: String ->
            val expected = sqlExpected[index].normalizeSQL()
            // Then
            Assert.assertEquals(expected, sql)
            index++
        }
    }

    private fun String.normalizeSQL() = this.trimIndent().replace("\n", "")
        .replace("\t", "").replace(",  ", ", ").replace("  ", "")
}