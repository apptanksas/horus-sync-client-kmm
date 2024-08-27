package com.apptank.horus.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

abstract class ServiceTest {
    companion object {
        const val BASE_URL = "http://localhost:8080"
    }


    fun createMockResponse(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { _ ->
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }


}