package org.apptank.horus.client

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.config.DATABASE_NAME
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.di.IDatabaseDriverFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class DatabaseDriverFactory(directory: Path) : IDatabaseDriverFactory, AutoCloseable {
    private val driver: SqlDriver
    init {
        Files.createDirectories(directory)
        driver = JdbcSqliteDriver(
            "jdbc:sqlite:${directory.resolve(DATABASE_NAME).toAbsolutePath()}",
            Properties().apply { setProperty("foreign_keys", "true") },
            HorusDatabase.Schema
        )
    }
    override fun getDriver(): SqlDriver = driver
    override fun getDatabase(): HorusDatabase = HorusDatabase(getDatabaseName(), driver)
    override fun getDatabaseName(): String = DATABASE_NAME
    override fun getSchema(): HorusDatabase.Schema = HorusDatabase.Schema
    override fun close() = driver.close()
}
