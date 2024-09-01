package com.apptank.horus.client.migration.network.service

import com.apptank.horus.client.MOCK_RESPONSE_GET_MIGRATION
import com.apptank.horus.client.ServiceTest
import com.apptank.horus.client.base.DataResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test


class MigrationServiceTest : ServiceTest() {

    @Test
    fun getMigrationIsSuccess() = runBlocking {
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_MIGRATION)
        val apiClient = MigrationService(mockEngine, BASE_URL)
        val response = apiClient.getMigration()

        Assert.assertTrue(response is DataResult.Success)
    }

    @Test
    fun getMigrationIsFailure() = runBlocking {
        val mockEngine = createMockResponse("{}", status = HttpStatusCode.InternalServerError)
        val apiClient = MigrationService(mockEngine, BASE_URL)
        val response = apiClient.getMigration()

        Assert.assertTrue(response is DataResult.Failure)
    }

    @Test
    fun getMigrationIsNotAuthorized() = runBlocking {
        val mockEngine = createMockResponse("{}", status = HttpStatusCode.Unauthorized)
        val apiClient = MigrationService(mockEngine, BASE_URL)
        val response = apiClient.getMigration()

        Assert.assertTrue(response is DataResult.NotAuthorized)
    }


}