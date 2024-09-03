package com.apptank.horus.client.base.network

import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.exception.UserNotAuthenticatedException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

internal abstract class BaseService(
    engine: HttpClientEngine,
    private val baseUrl: String
) {
    val decoderJson = Json { ignoreUnknownKeys = true }
    protected val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(decoderJson)
        }
    }

    protected suspend fun <T : Any> get(
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

    protected suspend fun <T : Any> post(
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

    private suspend fun <T : Any> handleResponse(
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

            if (responseText.isBlank()) {
                return DataResult.Success(Unit as T)
            }

            val responseParsed: T = onResponse(responseText)
            DataResult.Success(responseParsed)
        }.getOrElse {
            it.printStackTrace()
            DataResult.Failure(it)
        }.also {
            client.close()
        }
    }

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

    private fun buildUrl(path: String): String {
        return "$baseUrl/$path"
    }

    protected inline fun <reified R : Any> String.serialize() =
        decoderJson.decodeFromString<R>(this)
}