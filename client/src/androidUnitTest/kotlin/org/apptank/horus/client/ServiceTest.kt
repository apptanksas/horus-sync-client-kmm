package org.apptank.horus.client

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.network.HttpHeader
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.Assert
import org.junit.Before
import kotlin.test.assertTrue

abstract class ServiceTest : TestCase() {
    companion object {
        const val BASE_URL = "http://dev.api"
    }

    private lateinit var lastRequest: HttpRequestData
    private lateinit var lastRequestBody: ByteArray

    @Before
    fun setup() {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
    }

    fun createMockResponse(content: String? = null, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { request ->
            lastRequest = request

            lastRequestBody = request.body.toByteArray()
            validateUrl(request.url.toString())
            validateJsonBody(request.body.toByteArray())
            validateHeaders()

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

    protected fun assertRequestContainsQueryParam(queryParam: String, value: String) {
        Assert.assertEquals(
            "Query param \"$queryParam\" invalid",
            value,
            lastRequest.url.parameters[queryParam].toString()
        )
    }

    protected fun assertRequestMissingQueryParam(queryParam: String) {
        Assert.assertNull(
            "Query param \"$queryParam\" should not be present",
            lastRequest.url.parameters[queryParam]
        )
    }

    protected fun assertRequestHeader(header: String, value: String) {
        Assert.assertEquals("Header $header is invalid!", value, lastRequest.headers[header])
    }

    protected fun assertRequestBody(body: String) {
        Assert.assertEquals(body, String(lastRequestBody))
    }

    private fun validateHeaders() {
        assertRequestHeader(HttpHeader.ACCEPT, "application/json")
        assertRequestHeader(HttpHeader.AUTHORIZATION, "Bearer $USER_ACCESS_TOKEN")
        assertTrue(lastRequest.headers.contains(HttpHeader.X_REQUEST_ID))
    }


}