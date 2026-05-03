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

    @Volatile
    private var lastRequest: HttpRequestData? = null

    @Volatile
    private var lastRequestBody: ByteArray = ByteArray(0)

    @Before
    fun setup() {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        lastRequest = null
        lastRequestBody = ByteArray(0)
    }

    fun createMockResponse(content: String? = null, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { request ->
            lastRequest = request
            lastRequestBody = request.body.toByteArray()
            validateUrl(request.url.toString())
            validateJsonBody(lastRequestBody)
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
        val req = lastRequest ?: return
        val contentType = req.headers[HttpHeaders.ContentType] ?: ""
        // Skip JSON validation for multipart form data (e.g. file uploads)
        if (contentType.contains("multipart/form-data", ignoreCase = true)) return
        val bodyStr = String(body)
        // Also skip if body itself doesn't look like JSON (e.g. multipart boundary content)
        if (!bodyStr.startsWith("{") && !bodyStr.startsWith("[")) return
        Assert.assertTrue(
            "Invalid JSON: $bodyStr",
            (bodyStr.startsWith("{") && bodyStr.endsWith("}")) ||
                    (bodyStr.startsWith("[") && bodyStr.endsWith("]"))
        )
    }

    protected fun assertRequestContainsQueryParam(queryParam: String, value: String) {
        val req = requireNotNull(lastRequest) { "No HTTP request was captured. Make sure the service call completed before asserting." }
        Assert.assertEquals(
            "Query param \"$queryParam\" invalid",
            value,
            req.url.parameters[queryParam].toString()
        )
    }

    protected fun assertRequestMissingQueryParam(queryParam: String) {
        val req = requireNotNull(lastRequest) { "No HTTP request was captured. Make sure the service call completed before asserting." }
        Assert.assertNull(
            "Query param \"$queryParam\" should not be present",
            req.url.parameters[queryParam]
        )
    }

    protected fun assertRequestHeader(header: String, value: String) {
        val req = requireNotNull(lastRequest) { "No HTTP request was captured. Make sure the service call completed before asserting." }
        Assert.assertEquals("Header $header is invalid!", value, req.headers[header])
    }

    protected fun assertRequestBody(body: String) {
        Assert.assertEquals(body, String(lastRequestBody))
    }

    private fun validateHeaders() {
        val req = lastRequest ?: return
        Assert.assertEquals("Header ${HttpHeader.ACCEPT} is invalid!", "application/json", req.headers[HttpHeader.ACCEPT])
        Assert.assertEquals("Header ${HttpHeader.AUTHORIZATION} is invalid!", "Bearer $USER_ACCESS_TOKEN", req.headers[HttpHeader.AUTHORIZATION])
        assertTrue(req.headers.contains(HttpHeader.X_REQUEST_ID))
    }

}