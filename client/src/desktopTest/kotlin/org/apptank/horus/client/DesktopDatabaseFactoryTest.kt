package org.apptank.horus.client

import java.nio.file.Files
import app.cash.sqldelight.db.QueryResult
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopDatabaseFactoryTest {
    @Test fun persistsDatabaseAndEnforcesForeignKeys() {
        val directory = Files.createTempDirectory("horus-desktop-test")
        DatabaseDriverFactory(directory).use { factory ->
            val driver = factory.getDriver()
            driver.execute(null, "CREATE TABLE desktop_test (id INTEGER PRIMARY KEY)", 0)
            driver.execute(null, "INSERT INTO desktop_test VALUES (42)", 0)
            val enabled = driver.executeQuery(null, "PRAGMA foreign_keys", { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0))
            }, 0).value
            assertEquals(1L, enabled)
        }
        DatabaseDriverFactory(directory).use { factory ->
            val value = factory.getDriver().executeQuery(null, "SELECT id FROM desktop_test", { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0))
            }, 0).value
            assertEquals(42L, value)
        }
    }
}
