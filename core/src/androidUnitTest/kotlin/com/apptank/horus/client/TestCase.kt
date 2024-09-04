package com.apptank.horus.client

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.extensions.prepareSQLValueAsString
import io.ktor.utils.io.core.toByteArray
import kotlinx.datetime.Clock
import org.kotlincrypto.hash.sha2.SHA256
import java.util.UUID
import kotlin.random.Random

abstract class TestCase {


    protected fun SqlDriver.insertOrThrow(table: String, values: Map<String, Any>) {
        val columns = values.keys.joinToString(", ")
        val valuesString = values.values.joinToString(", ") { it.prepareSQLValueAsString() }
        val query = "INSERT INTO $table ($columns) VALUES ($valuesString);"

        if (execute(null, query, 0).value == 0L) {
            throw IllegalStateException("Insertion failed")
        }
    }

    protected fun SqlDriver.createTable(table: String, columns: Map<String, String>) {
        val columnsString = columns.entries.joinToString(", ") { (name, type) -> "$name $type" }
        val query = "CREATE TABLE $table ($columnsString);"
        execute(null, query, 0)
    }

    protected fun <T> generateArray(size: Int = 10, creator: () -> T): List<T> {
        val sizeList = Random.nextInt(1, size)
        return List(sizeList) { Random.nextInt(0, size) }.map {
            creator()
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

    private fun sha256(input: String): String {
        val sha256 = SHA256()
        val hashBytes = sha256.digest(input.toByteArray())
        return hashBytes.joinToString("") { it.toHex() }
    }

    private fun Byte.toHex(): String = this.toUByte().toString(16).padStart(2, '0')

    companion object {
        const val USER_ACCESS_TOKEN =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI5Y2VhN2MyZS1jNTgwLTQ3ODEtYTYxOS01ZmQ3ZTMzODFlYTgiLCJqdGkiOiJhOTY1MTYyMDM4ZGVjMjdhMjNiYjM1MGNlMzAzYWJkNmRmMTFiMjQ1YjQ0MWFiYjIzOWI3Mjg1YjJkYzI1NWNmMmQ4YzczOTZjZGU1NTQxZiIsImlhdCI6MTcyNTMwOTE5OC4xNDkxNjUsIm5iZiI6MTcyNTMwOTE5OC4xNDkxNjYsImV4cCI6MTc1Njg0NTE5OC4xNDcwODQsInN1YiI6ImRiMDVmOTYwLWVlNWQtNDA0ZS05OTI5LTViZWI0OGE2MTJhMSIsInNjb3BlcyI6WyJ1c2VyLnNpZ251cCIsInVzZXIucHJvZmlsZS5yZWFkIiwidXNlci5hdXRoZW50aWNhdGUiLCJ1c2VyLmludml0YXRpb24uY3JlYXRlIiwidXNlci5wYXNzd29yZC5yZWNvdmVyeSIsInVzZXIucHJvZmlsZS5jcmVhdGUiLCJ1c2VyLnBhc3N3b3JkLnVwZGF0ZSIsInVzZXIucHJvZmlsZS51cGRhdGUiLCJ1c2VyLnByb2ZpbGUuZGVsZXRlIiwiZGF0YS5zeW5jIl0sImVudGl0aWVzX2dyYW50ZWQiOlt7InVzZXJfb3duZXJfaWQiOiI3YTc2ODQ3NC1kZTEyLTQzNjgtOWVkMS0zNzUyZGE4MDBkNjAiLCJlbnRpdHlfaWQiOiI2ZDc3NjdlYy0wOWJmLTRlZDgtOTZkOS05NTU2ZTRmMjExNDQiLCJlbnRpdHlfbmFtZSI6ImxldmkzNiIsImFjY2Vzc19sZXZlbCI6IlJDVUQifV19.D9ANGMsIvTnLFr5yn8PiuPWExZfofkgwoPibDsfVhJWZVT2wbU-N1K8qpFBsp_4PMopvelMZPyc28b8jU5ZZthY36FjM93oC0Xa9CtKc8cnY2qFSnP3XZ7tpYndloIkPqax53AApojCS3mJV_swbilTtisrS3bwoZzt8CKgTdHm0cpmPj06VGCbGBx-Rk1Y24KCMRONSRiJBiiTo7Oyi3kOw1Xv7G9r6WtR44wz2dEqK6PN9S2tK9tCLKVx1y_Wq6PZZXCuK83VFuycCBgLTXivNRTVgoOSxfTMwTcLVG_TtsVECdjpL4PpKoa3NAuTtiG9Xntx8wl-MNbWqfZpY5k3Kc33grsCkYHlJOysBgCjzRHkBivNK0Z6YUcTuAkR8Yz-2AwZ7eDm9ZJvjmafq5N_EKetNBegtw7jG_6UMbqLr9dSyxOi5FBUTeaoccckYxhoMiXSRRLq5Abi_DodMoBklF2P7ACG1pT-iMKECO8GynoaTknyO116NN0MJpoyUUJL1GeYW2p3wzWucV_Wf9g4JZHtwb_KTwvBJOK2rQCQ3ZvmaOwQQEQfwnSD2oRmlDIr0uGNf1_q9JvPvd3aXhSwLr34k7QEx1vpX0JNiiVx8Jyh4pJLYitZyuTh9DjvG1kciYniMDnqVpVKOdo7aNb-2MDUsS3zVckdLpxiDSy0"
    }
}