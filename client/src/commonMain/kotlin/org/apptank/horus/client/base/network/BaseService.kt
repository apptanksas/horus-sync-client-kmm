package org.apptank.horus.client.base.network

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.exception.UserNotAuthenticatedException
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.logException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.apptank.horus.client.sync.upload.data.FileData

/**
 * BaseService provides a foundational layer for making HTTP requests with a shared HttpClient.
 * It supports GET and POST methods, handles responses, and manages authentication headers.
 *
 * The class is designed to work with an engine passed via constructor and a base URL for all requests.
 *
 * @param engine The HttpClientEngine used for network requests.
 * @param baseUrl The base URL for the API endpoints.
 *
 * @author John Ospina
 * @year 2024
 */
internal abstract class BaseService(
    engine: HttpClientEngine,
    private val baseUrl: String
) {
    // JSON decoder configured to ignore unknown keys
    val decoderJson = Json { ignoreUnknownKeys = true }

    // HttpClient instance configured with content negotiation for JSON
    protected val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(decoderJson)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    info(message)
                }
            }
            level = LogLevel.ALL
        }
    }

    /**
     * Makes a GET request to the specified path with optional query parameters.
     *
     * @param path The endpoint path to make the request to.
     * @param queryParams A map of query parameters to append to the request URL.
     * @param onResponse A lambda function to process the response body into the expected type.
     * @return A DataResult containing either the result of the request or an error.
     */
    protected suspend inline fun <reified T : Any> get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        onResponse: (response: String) -> T
    ): DataResult<T> {
        return handleResponse(
            response = client.get(buildUrl(path)) {
                contentType(ContentType.Application.Json)
                url {
                    queryParams.forEach { (key, value) ->
                        parameters.append(key, value)
                    }
                }
                setupHeaders(this)
            }, onResponse
        )
    }

    /**
     * Makes a POST request to the specified path with the provided data.
     *
     * @param path The endpoint path to make the request to.
     * @param data The data to be sent as the request body.
     * @param onResponse A lambda function to process the response body into the expected type.
     * @return A DataResult containing either the result of the request or an error.
     */
    protected suspend inline fun <reified T : Any> post(
        path: String,
        data: Any,
        onResponse: (response: String) -> T
    ): DataResult<T> {
        return handleResponse(client.post(buildUrl(path)) {
            contentType(ContentType.Application.Json)
            setBody(data)
            setupHeaders(this)
        }, onResponse)
    }

    /**
     * Makes a POST request to the specified path with the provided data and binary data.
     *
     * @param path The endpoint path to make the request to.
     * @param data The data to be sent as the request body.
     * @param onResponse A lambda function to process the response body into the expected type.
     * @param onProgressUpload A lambda function to track the progress of the upload.
     * @return A DataResult containing either the result of the request or an error.
     */
    protected suspend inline fun <reified T : Any> postWithMultipartFormData(
        path: String,
        data: Map<String, Any>,
        onResponse: (response: String) -> T,
        crossinline onProgressUpload: (Int) -> Unit = {}
    ): DataResult<T> {
        return handleResponse(client.post(buildUrl(path)) {
            contentType(ContentType.Application.Json)
            setBody(MultiPartFormDataContent(formData {
                parseFormData(data)
            }))
            setupHeaders(this)
            onUpload { bytesSentTotal, contentLength ->
                if (contentLength > 0) {
                    onProgressUpload(((bytesSentTotal.toDouble() / contentLength.toDouble()) * 100).toInt())
                }
            }
        }, onResponse)
    }


    /**
     * Handles the HTTP response by checking for status codes, parsing the response body, and managing errors.
     *
     * @param response The HttpResponse object received from the network request.
     * @param onResponse A lambda function to process the response body into the expected type.
     * @return A DataResult containing either the success result or failure information.
     */
    private suspend inline fun <reified T : Any> handleResponse(
        response: HttpResponse,
        onResponse: (response: String) -> T
    ): DataResult<T> {
        return kotlin.runCatching {

            if (response.status.value == 401 || response.status.value == 403) {
                return DataResult.NotAuthorized(Exception("Unauthorized"))
            }

            if (!response.status.isSuccess()) {
                return DataResult.Failure(Exception("Error: ${response.status.value}"))
            }

            val responseText = response.bodyAsText()

            if (responseText.responseIsEmpty() && T::class == List::class) {
                return DataResult.Success(onResponse("[]"))
            }

            if (responseText.responseIsEmpty()) {
                return DataResult.Success(onResponse("{}"))
            }

            val responseParsed: T = onResponse(responseText)
            DataResult.Success(responseParsed)
        }.getOrElse {
            it.printStackTrace()
            DataResult.Failure(it)
        }
    }

    /**
     * Sets up the necessary headers for the request, including authentication and acting as another user.
     *
     * @param builder The HttpRequestBuilder used to configure the request.
     * @return The modified HttpRequestBuilder with added headers.
     */
    private fun setupHeaders(builder: HttpRequestBuilder): HttpRequestBuilder {
        builder.headers {
            append(HttpHeader.CONTENT_TYPE, "application/json")
            append(HttpHeader.ACCEPT, "application/json")

            with(HorusAuthentication) {
                if (isNotUserAuthenticated()) {
                    throw UserNotAuthenticatedException()
                }
                append(HttpHeader.AUTHORIZATION, "Bearer ${getUserAccessToken()}")

                if (isUserActingAs()) {
                    append(HttpHeader.USER_ACTING, getActingAsUserId())
                }
            }

        }
        return builder
    }

    /**
     * Builds the full URL for the request by appending the given path to the base URL.
     *
     * @param path The path to append to the base URL.
     * @return The full URL as a String.
     */
    protected fun buildUrl(path: String): String {
        return "$baseUrl/$path"
    }

    /**
     * Extension function to deserialize a JSON string into an object of the specified type.
     *
     * @return The deserialized object of type R.
     */
    protected inline fun <reified R : Any> String.serialize() =
        decoderJson.decodeFromString<R>(this)

    /**
     * Extension function to check if a response body is empty or contains only whitespace.
     * This is used to determine if the response is valid or not.
     *
     * @return True if the response is empty or contains only whitespace, false otherwise.
     */
    private fun String.responseIsEmpty(): Boolean {
        return this.isBlank() || this == "{}" || this == "[]"
    }

    /**
     * Extension function to parse a map of data into a FormBuilder for multipart form data.
     *
     * @param data The map of data to parse into the form.
     * @return The FormBuilder with the data appended.
     */
    private fun FormBuilder.parseFormData(data: Map<String, Any>): FormBuilder {
        data.forEach { (key, value) ->
            when (value) {
                is String -> append(key, value)
                is Int -> append(key, value)
                is Long -> append(key, value)
                is Float -> append(key, value)
                is Double -> append(key, value)
                is Boolean -> append(key, value)
                is ByteArray -> append(key, value)
                is FileData -> append(key, value.data, Headers.build {
                    append(HttpHeaders.ContentType, "multipart/form-data")
                    append(HttpHeaders.ContentDisposition, "filename=\"${value.filename}\"")
                })

                else -> logException("Unsupported data type for key: $key")
            }
        }
        return this
    }
}
