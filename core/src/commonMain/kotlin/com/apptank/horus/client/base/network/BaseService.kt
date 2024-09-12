package com.apptank.horus.client.base.network

import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.exception.UserNotAuthenticatedException
import com.apptank.horus.client.extensions.info
import com.apptank.horus.client.extensions.logException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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

            if(responseText.responseIsEmpty()) {
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
    private fun buildUrl(path: String): String {
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
}
