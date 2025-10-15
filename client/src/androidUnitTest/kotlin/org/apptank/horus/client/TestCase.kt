package org.apptank.horus.client

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.control.scheme.EntitiesTable
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.extensions.createSQLInsertOrReplace
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.extensions.prepareSQLValueAsString
import org.apptank.horus.client.di.IDatabaseDriverFactory
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.migration.network.service.IMigrationService
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.apptank.horus.client.tasks.RetrieveDatabaseSchemeTask
import org.apptank.horus.client.tasks.SynchronizeInitialDataTask
import org.apptank.horus.client.tasks.ValidateHashingTask
import org.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
import com.russhwolf.settings.MapSettings
import io.ktor.utils.io.core.toByteArray
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.toFile
import io.mockative.Matchers
import io.mockative.classOf
import io.mockative.matchers.Matcher
import io.mockative.mock
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.config.UploadFilesConfig
import org.apptank.horus.client.control.helper.IDataSharedDatabaseHelper
import org.apptank.horus.client.control.scheme.EntityAttributesTable
import org.apptank.horus.client.extensions.normalizePath
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.sync.upload.data.FileMimeTypeGroup
import org.apptank.horus.client.tasks.RetrieveDataSharedTask
import org.apptank.horus.client.tasks.SynchronizeDataTask
import org.junit.After
import org.kotlincrypto.hash.sha2.SHA256
import java.nio.file.Paths
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertTrue

abstract class TestCase {

    @After
    fun tearDownEnd() {
        clearLocalPathStorage()
    }

    private fun clearLocalPathStorage() {
        val basePath = getLocalTestPath().toFile()
        if (!basePath.exists()) {
            return
        }

        basePath.removeAll()
    }

    private fun File.removeAll() {
        if (this.isDirectory) {
            this.listFiles()?.forEach { it.removeAll() }
            this.deleteRecursively()
        }
        if (exists()) {
            this.delete()
        }
    }

    internal fun createFileInLocalStorage(pathFile: String, content: String = "test"): String {
        val filename = pathFile.substringAfterLast("/")
        val path = pathFile.replace(filename, "")
        val baseFilePath = if (pathFile.contains(getLocalTestPath().normalizePath())) {
            path.toFile()
        } else {
            getLocalTestPath().toFile()
        }

        if (!baseFilePath.exists()) {
            baseFilePath.mkdirs()
        }

        val file = baseFilePath.resolve(filename)

        if (file.createNewFile() && file.setWritable(true)) {
            file.writeBytes(content.toByteArray())
        }
        return file.absolutePath
    }

    protected fun getHorusConfigTest(): HorusConfig {
        return HorusConfig(
            "http://dev.horus.com", UploadFilesConfig(
                getLocalTestPath(),
                FileMimeTypeGroup.IMAGES_PNG + FileMimeTypeGroup.IMAGES_JPEG,
                1024 * 5 // 5MB
            )
        )
    }

    internal fun getMockValidateHashingTask(): ValidateHashingTask {
        return ValidateHashingTask(
            mock(classOf<ISyncControlDatabaseHelper>()),
            mock(classOf<ISynchronizationService>()),
            getMockValidateMigrationTask()
        )
    }

    internal fun getMockValidateMigrationTask(): ValidateMigrationLocalDatabaseTask {
        return ValidateMigrationLocalDatabaseTask(
            MapSettings(),
            mock(classOf<IDatabaseDriverFactory>()),
            getMockRetrieveDatabaseSchemeTask()
        )
    }

    internal fun getMockRetrieveDatabaseSchemeTask(): RetrieveDatabaseSchemeTask {
        return RetrieveDatabaseSchemeTask(
            mock(classOf<IMigrationService>())
        )
    }

    internal fun getMockSynchronizeInitialDataTask(): SynchronizeInitialDataTask {
        return SynchronizeInitialDataTask(
            mock(classOf<INetworkValidator>()),
            mock(classOf<IOperationDatabaseHelper>()),
            mock(classOf<ISyncControlDatabaseHelper>()),
            mock(classOf<ISynchronizationService>()),
            getMockValidateHashingTask()
        )
    }

    internal fun getMockSynchronizeDataTask(): SynchronizeDataTask {
        return SynchronizeDataTask(
            mock(classOf<INetworkValidator>()),
            mock(classOf<ISyncControlDatabaseHelper>()),
            mock(classOf<IOperationDatabaseHelper>()),
            mock(classOf<ISynchronizationService>()),
            getMockSynchronizeInitialDataTask()
        )
    }

    internal fun getMockRetrieveDataSharedTask(): RetrieveDataSharedTask {
        return RetrieveDataSharedTask(
            MapSettings(),
            mock(classOf<INetworkValidator>()),
            mock(classOf<IDataSharedDatabaseHelper>()),
            mock(classOf<ISynchronizationService>()),
            getMockSynchronizeDataTask()
        )
    }


    protected fun SqlDriver.insertOrThrow(table: String, values: Map<String, Any>) {
        val columns = values.keys.joinToString(", ")
        val valuesString = values.values.joinToString(", ") { it.prepareSQLValueAsString() }
        val query = "INSERT INTO $table ($columns) VALUES ($valuesString);"

        if (execute(null, query, 0).value == 0L) {
            throw IllegalStateException("Insertion failed")
        }
    }

    protected fun SqlDriver.createTable(table: String, columns: Map<String, String>, custom: List<String> = emptyList()) {
        val columnsString = columns.entries.joinToString(", ") { (name, type) -> "$name $type" }
        val customString = custom.joinToString(", ")
        var query = "CREATE TABLE $table ($columnsString"

        if (customString.isNotEmpty()) {
            query += ", $customString"
        }
        query += ");"

        execute(null, query, 0)
    }

    protected fun <T> SqlDriver.rawQuery(query: String, mapper: (SqlCursor) -> T?): List<T> {
        return executeQuery(null, query, {
            val resultList = mutableListOf<T>()
            while (it.next().value) {
                mapper(it)?.let { item ->
                    resultList.add(item)
                }
            }
            QueryResult.Value(resultList)
        }, 0).value
    }

    protected fun <T> generateRandomArray(size: Int = 10, creator: (Int) -> T): List<T> {
        val sizeList = Random.nextInt(1, size)
        return List(sizeList) { Random.nextInt(0, size) }.mapIndexed { index, i ->
            creator(index)
        }
    }

    protected fun <T> generateArray(size: Int = 10, creator: (Int) -> T): List<T> {
        return List(size) { Random.nextInt(0, size) }.mapIndexed { index, i ->
            creator(index)
        }
    }

    protected fun SqlDriver.getTablesNames(): List<String> {
        return rawQuery(QUERY_TABLES) {
            it.getString(0)
        }
    }

    protected fun uuid(): String {
        return UUID.randomUUID().toString()
    }

    protected fun timestamp(): Long {
        return Clock.System.now().toEpochMilliseconds() / 1000
    }

    protected fun randomHash(): String {
        return sha256(uuid())
    }

    protected fun callbackMatcher(): Callback {
        return Matchers.enqueue(object : Matcher<Callback> {
            override val placeholder: Callback = {}
            override fun matches(value: Any?): Boolean {
                // Validate is a function then execute
                if (value is Function<*>) {
                    (value as Callback).invoke()
                    return true
                }
                return false
            }

        })
    }

    protected fun SqlDriver.registerEntity(entity: String, isWritable: Boolean = true, level: Int = 0) {
        execute(
            createSQLInsertOrReplace(
                EntitiesTable.TABLE_NAME,
                EntitiesTable.mapToCreate(entity, isWritable, level)
            )
        )
    }

    protected fun SqlDriver.registerEntityAttribute(
        entity: String,
        attributeName: String,
        attributeType: AttributeType
    ) {
        execute(
            createSQLInsertOrReplace(
                EntityAttributesTable.TABLE_NAME,
                EntityAttributesTable.mapToCreate(entity, attributeName, attributeType)
            )
        )
    }

    private fun sha256(input: String): String {
        val sha256 = SHA256()
        val hashBytes = sha256.digest(input.toByteArray())
        return hashBytes.joinToString("") { it.toHex() }
    }

    private fun Byte.toHex(): String = this.toUByte().toString(16).padStart(2, '0')

    protected fun getLocalTestPath(path: String? = null): String {
        val basePath = Paths.get("").toAbsolutePath().toString() + LOCAL_PATH
        if (path != null) {
            return "$basePath$path"
        }
        return basePath
    }

    protected fun coAssertThrows(clazz: Class<out Throwable>, block: suspend () -> Unit) {
        runBlocking {
            try {
                block()
                throw AssertionError("Expected exception of type ${clazz.simpleName} but none was thrown")
            } catch (e: Throwable) {
                if (clazz.isInstance(e)) {
                    assertTrue(clazz.isInstance(e))
                    return@runBlocking
                }
                throw AssertionError("Expected exception of type ${clazz.simpleName} but got ${e::class.simpleName}")
            }
        }
    }

    companion object {
        const val USER_ACCESS_TOKEN =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI5Y2VhN2MyZS1jNTgwLTQ3ODEtYTYxOS01ZmQ3ZTMzODFlYTgiLCJqdGkiOiJhOTY1MTYyMDM4ZGVjMjdhMjNiYjM1MGNlMzAzYWJkNmRmMTFiMjQ1YjQ0MWFiYjIzOWI3Mjg1YjJkYzI1NWNmMmQ4YzczOTZjZGU1NTQxZiIsImlhdCI6MTcyNTMwOTE5OC4xNDkxNjUsIm5iZiI6MTcyNTMwOTE5OC4xNDkxNjYsImV4cCI6MTc1Njg0NTE5OC4xNDcwODQsInN1YiI6ImRiMDVmOTYwLWVlNWQtNDA0ZS05OTI5LTViZWI0OGE2MTJhMSIsInNjb3BlcyI6WyJ1c2VyLnNpZ251cCIsInVzZXIucHJvZmlsZS5yZWFkIiwidXNlci5hdXRoZW50aWNhdGUiLCJ1c2VyLmludml0YXRpb24uY3JlYXRlIiwidXNlci5wYXNzd29yZC5yZWNvdmVyeSIsInVzZXIucHJvZmlsZS5jcmVhdGUiLCJ1c2VyLnBhc3N3b3JkLnVwZGF0ZSIsInVzZXIucHJvZmlsZS51cGRhdGUiLCJ1c2VyLnByb2ZpbGUuZGVsZXRlIiwiZGF0YS5zeW5jIl0sImVudGl0aWVzX2dyYW50ZWQiOlt7InVzZXJfb3duZXJfaWQiOiI3YTc2ODQ3NC1kZTEyLTQzNjgtOWVkMS0zNzUyZGE4MDBkNjAiLCJlbnRpdHlfaWQiOiI2ZDc3NjdlYy0wOWJmLTRlZDgtOTZkOS05NTU2ZTRmMjExNDQiLCJlbnRpdHlfbmFtZSI6ImxldmkzNiIsImFjY2Vzc19sZXZlbCI6IlJDVUQifV19.D9ANGMsIvTnLFr5yn8PiuPWExZfofkgwoPibDsfVhJWZVT2wbU-N1K8qpFBsp_4PMopvelMZPyc28b8jU5ZZthY36FjM93oC0Xa9CtKc8cnY2qFSnP3XZ7tpYndloIkPqax53AApojCS3mJV_swbilTtisrS3bwoZzt8CKgTdHm0cpmPj06VGCbGBx-Rk1Y24KCMRONSRiJBiiTo7Oyi3kOw1Xv7G9r6WtR44wz2dEqK6PN9S2tK9tCLKVx1y_Wq6PZZXCuK83VFuycCBgLTXivNRTVgoOSxfTMwTcLVG_TtsVECdjpL4PpKoa3NAuTtiG9Xntx8wl-MNbWqfZpY5k3Kc33grsCkYHlJOysBgCjzRHkBivNK0Z6YUcTuAkR8Yz-2AwZ7eDm9ZJvjmafq5N_EKetNBegtw7jG_6UMbqLr9dSyxOi5FBUTeaoccckYxhoMiXSRRLq5Abi_DodMoBklF2P7ACG1pT-iMKECO8GynoaTknyO116NN0MJpoyUUJL1GeYW2p3wzWucV_Wf9g4JZHtwb_KTwvBJOK2rQCQ3ZvmaOwQQEQfwnSD2oRmlDIr0uGNf1_q9JvPvd3aXhSwLr34k7QEx1vpX0JNiiVx8Jyh4pJLYitZyuTh9DjvG1kciYniMDnqVpVKOdo7aNb-2MDUsS3zVckdLpxiDSy0"
        const val QUERY_TABLES = "SELECT name FROM sqlite_master WHERE type='table';"
        const val LOCAL_PATH = "/test/path/"
    }
}