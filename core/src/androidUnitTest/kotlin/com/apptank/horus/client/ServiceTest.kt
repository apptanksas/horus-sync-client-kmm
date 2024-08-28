package com.apptank.horus.client

import com.apptank.horus.client.base.network.HttpHeader
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.Assert

abstract class ServiceTest {
    companion object {
        const val BASE_URL = "http://localhost:8080"
    }


    fun createMockResponse(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { request ->

            // Validate headers request
            //Assert.assertNotNull(request.headers[HttpHeader.AUTHORIZATION])

            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }


}