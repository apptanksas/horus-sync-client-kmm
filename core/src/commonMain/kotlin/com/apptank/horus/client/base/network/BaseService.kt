package com.apptank.horus.client.base.network

import com.apptank.horus.client.base.DataResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

abstract class BaseService(
    engine: HttpClientEngine
) {
    val decoderJson = Json { ignoreUnknownKeys = true }
    protected val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(decoderJson)
        }
    }

    protected fun generateUrl(
        baseUrl: String,
        path: String,
        queryParams: Map<String, String>
    ): String {
        var url = "$baseUrl/$path"
        if (queryParams.isNotEmpty()) {
            url += "?"
            queryParams.forEach { (key, value) ->
                url += "$key=$value&"
            }
            url = url.dropLast(1)
        }
        return url
    }

    protected suspend fun <T : Any> get(
        url: String,
        onResponse: (response: String) -> T
    ): DataResult<T> {
        return kotlin.runCatching {
            val response = client.get(url) {
                contentType(ContentType.Application.Json)
            }
            if (response.status.value == 401 || response.status.value == 403) {
                return DataResult.NotAuthorized(Exception("Unauthorized"))
            }
            val responseParsed: T = onResponse(response.bodyAsText())
            DataResult.Success(responseParsed)
        }.getOrElse {
            it.printStackTrace()
            DataResult.Failure(it)
        }
    }

    protected inline fun <reified R : Any> String.serialize() =
        decoderJson.decodeFromString<R>(this)
}