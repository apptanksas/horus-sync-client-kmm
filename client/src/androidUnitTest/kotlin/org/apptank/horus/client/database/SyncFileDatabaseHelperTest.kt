package org.apptank.horus.client.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.scheme.SyncFileTable
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.generateSyncControlFile
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class SyncFileDatabaseHelperTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var databaseHelper: SyncFileDatabaseHelper

    @Before
    fun before() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        databaseHelper = SyncFileDatabaseHelper("database", driver)
        HorusDatabase.Schema.create(driver)
    }


    @Test
    fun insertIsSuccess() {
        // Given
        val file = generateSyncControlFile()

        // When
        databaseHelper.insert(file)

        // Then
        val referenceResult = driver.executeQuery(
            null,
            SimpleQueryBuilder(SyncFileTable.TABLE_NAME).where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        SyncFileTable.ATTR_REFERENCE,
                        file.reference
                    )
                )
            ).build(), {
                QueryResult.Value(it.getString(0))
            }, 0
        ).value

        Assert.assertEquals(file.reference, referenceResult)
    }

    @Test
    fun insertMultipleSuccess() {
        // Given
        val file1 = generateSyncControlFile()
        val file2 = generateSyncControlFile()

        // When
        databaseHelper.insert(file1, file2)

        // Then
        val referenceResult1 = driver.executeQuery(
            null,
            SimpleQueryBuilder(SyncFileTable.TABLE_NAME).where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        SyncFileTable.ATTR_REFERENCE,
                        file1.reference
                    )
                )
            ).build(), {
                QueryResult.Value(it.getString(0))
            }, 0
        ).value

        val referenceResult2 = driver.executeQuery(
            null,
            SimpleQueryBuilder(SyncFileTable.TABLE_NAME).where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        SyncFileTable.ATTR_REFERENCE,
                        file2.reference
                    )
                )
            ).build(), {
                QueryResult.Value(it.getString(0))
            }, 0
        ).value

        Assert.assertEquals(file1.reference, referenceResult1)
        Assert.assertEquals(file2.reference, referenceResult2)
    }

    @Test
    fun updateIsSuccess() {
        // Given

        val file = generateSyncControlFile()
        databaseHelper.insert(file)

        // When
        val newFile = SyncControl.File(
            file.reference,
            SyncControl.FileType.IMAGE,
            SyncControl.FileStatus.REMOTE,
            "pdf/png",
            "https://xxfast.github.io/KStore/using-platform-paths.html#on-desktop-jvm",
            "urlRemote"
        )
        val isUpdated = databaseHelper.update(newFile)

        // Then
        Assert.assertTrue(isUpdated)
        val urlLocalResult = driver.executeQuery(
            null,
            SimpleQueryBuilder(SyncFileTable.TABLE_NAME).where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        SyncFileTable.ATTR_REFERENCE,
                        file.reference
                    )
                )
            ).build(), {
                // Get url local
                QueryResult.Value(it.getString(3))
            }, 0
        ).value

        Assert.assertEquals(newFile.urlLocal, urlLocalResult)
    }

    @Test
    fun updateIsFailure() {
        // Given
        val newFile = generateSyncControlFile()

        // When
        val isUpdated = databaseHelper.update(newFile)

        // Then
        Assert.assertFalse(isUpdated)
    }

    @Test
    fun deleteIsSuccess() {
        // Given
        val file = generateSyncControlFile()
        databaseHelper.insert(file)

        // When
        val isDeleted = databaseHelper.delete(file)

        // Then
        Assert.assertTrue(isDeleted)

        val referenceResult = driver.executeQuery(
            null,
            SimpleQueryBuilder(SyncFileTable.TABLE_NAME).where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        SyncFileTable.ATTR_REFERENCE,
                        file.reference
                    )
                )
            ).build(), {
                QueryResult.Value(it.getString(0))
            }, 0
        ).value

        Assert.assertNull(referenceResult)
    }

    @Test
    fun deleteIsFailure() {
        // Given
        val file = generateSyncControlFile()

        // When
        val isDeleted = databaseHelper.delete(file)

        // Then
        Assert.assertFalse(isDeleted)
    }

    @Test
    fun searchIsSuccess() {
        // Given
        val file = generateSyncControlFile()
        databaseHelper.insert(file)

        // When
        val result = databaseHelper.search(file.reference)

        // Then
        Assert.assertNotNull(result)
        Assert.assertEquals(file.reference, result?.reference)
    }

    @Test
    fun searchIsFailure() {
        // Given
        val file = generateSyncControlFile()

        // When
        val result = databaseHelper.search(file.reference)

        // Then
        Assert.assertNull(result)
    }

    @Test
    fun searchBatchIsSuccess() {
        // Given
        val file1 = generateSyncControlFile()
        val file2 = generateSyncControlFile()
        databaseHelper.insert(file1, file2)

        // When
        val result = databaseHelper.searchBatch(listOf(file1.reference, file2.reference))
        val referencesResult = result.map { it.reference }

        // Then
        Assert.assertEquals(2, result.size)
        Assert.assertTrue(referencesResult.contains(file1.reference))
        Assert.assertTrue(referencesResult.contains(file2.reference))
    }

    @Test
    fun searchBatchIsFailure() {
        // Given
        val file1 = generateSyncControlFile()
        val file2 = generateSyncControlFile()

        // When
        val result = databaseHelper.searchBatch(listOf(file1.reference, file2.reference))

        // Then
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun queryByStatusIsSuccess() {
        // Given
        val status = SyncControl.FileStatus.REMOTE
        val file1 = generateSyncControlFile(status)
        val file2 = generateSyncControlFile(status)
        databaseHelper.insert(file1, file2)

        // When
        val result = databaseHelper.queryByStatus(status)

        // Then
        Assert.assertEquals(2, result.size)
        Assert.assertEquals(SyncControl.FileStatus.REMOTE, result[0].status)
        Assert.assertEquals(SyncControl.FileStatus.REMOTE, result[1].status)
    }

    @Test
    fun queryByStatusIsEmpty() {
        // Given
        val status = SyncControl.FileStatus.REMOTE
        val file1 = generateSyncControlFile(SyncControl.FileStatus.LOCAL)
        val file2 = generateSyncControlFile(SyncControl.FileStatus.LOCAL)
        databaseHelper.insert(file1, file2)

        // When
        val result = databaseHelper.queryByStatus(status)

        // Then
        Assert.assertTrue(result.isEmpty())
    }
}