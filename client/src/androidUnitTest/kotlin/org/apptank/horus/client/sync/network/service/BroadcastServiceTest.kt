package org.apptank.horus.client.sync.network.service

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.ServiceTest
import org.apptank.horus.client.base.DataResult
import org.junit.Test
import kotlin.test.assertEquals

class BroadcastServiceTest : ServiceTest() {

    @Test
    fun postAuthIsSuccess() = runBlocking {
        // Given
        val socketId = "627043405.514733093"
        val channelName = "private-horus.sync.72df0bb6-e1e8-43d5-95dc-af9b450ddf96"
        val expectedAuth = "Dz1LNRitjmTn664bpBkg1qRj:c6bfe96546578b1afd2458d3d0d3ca94fdbce1ffaf874f24dc589cfb70f25cfb"

        val responseBody = """
            {
                "auth": "$expectedAuth"
            }
        """.trimIndent()

        val mockEngine = createMockResponse(responseBody)
        val service = BroadcastService(mockEngine, BASE_URL)

        // When
        val response = service.postAuth(socketId, channelName)

        // Then
        assert(response is DataResult.Success)
        if (response is DataResult.Success) {
            assertEquals(expectedAuth, response.data.auth)
        }

        // Verify request body (form url encoded)
        assertRequestBody("socket_id=627043405.514733093&channel_name=private-horus.sync.72df0bb6-e1e8-43d5-95dc-af9b450ddf96")
    }

    @Test
    fun postAuthIsFailure() = runBlocking {
        // Given
        val mockEngine = createMockResponse("{}", status = HttpStatusCode.InternalServerError)
        val service = BroadcastService(mockEngine, BASE_URL)

        // When
        val response = service.postAuth("any", "any")

        // Then
        assert(response is DataResult.Failure)
    }
}
