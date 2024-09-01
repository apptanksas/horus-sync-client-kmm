package com.apptank.horus.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.Assert

abstract class ServiceTest : TestCase() {
    companion object {
        const val BASE_URL = "http://dev.api"
    }

    private lateinit var lastRequest: HttpRequestData
    private lateinit var lastRequestBody: ByteArray

    fun createMockResponse(content: String? = null, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { request ->
            lastRequest = request

            lastRequestBody = request.body.toByteArray()
            validateUrl(request.url.toString())
            validateJsonBody(request.body.toByteArray())

            respond(
                content = content ?: "",
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }


    private fun validateUrl(url: String) {
        val regex =
            Regex("^[(http(s)?){1}:\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)\$")
        Assert.assertTrue("Invalid URL: $url", regex.matches(url))
    }

    private fun validateJsonBody(body: ByteArray) {
        if (body.isEmpty()) return
        val body = String(body)
        Assert.assertTrue(
            "Invalid JSON: $body",
            (body.startsWith("{") && body.endsWith("}")) ||
                    (body.startsWith("[") && body.endsWith("]"))
        )
    }

    protected fun assertRequestQueryParams(queryParam: String, value: String) {
        Assert.assertEquals(value, lastRequest.url.parameters[queryParam])
    }

    protected fun assertRequestHeader(header: String, value: String) {
        Assert.assertEquals(value, lastRequest.headers[header])
    }

    protected fun assertRequestBody(body: String) {
        Assert.assertEquals(body, String(lastRequestBody))
    }


}