package org.apptank.horus.client.migration.network.service

import org.apptank.horus.client.MOCK_RESPONSE_GET_MIGRATION
import org.apptank.horus.client.ServiceTest
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.fold
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
        response.fold(
            onSuccess = {
                Assert.assertEquals(3, it.size)
                it.forEach {
                    Assert.assertNotNull(it.entity)
                    Assert.assertFalse(it.attributes?.isEmpty() ?: true)
                    Assert.assertNotNull(it.type)
                    Assert.assertNotNull(it.currentVersion)
                }
                Assert.assertTrue(it.get(1).getRelated().isNotEmpty())
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
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