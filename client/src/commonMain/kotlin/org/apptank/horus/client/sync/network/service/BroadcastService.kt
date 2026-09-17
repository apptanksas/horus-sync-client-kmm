package org.apptank.horus.client.sync.network.service

import io.ktor.client.engine.HttpClientEngine
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.network.BaseService
import org.apptank.horus.client.sync.network.dto.BroadcastDTO

/**
 * Implementation of the [IBroadcastService] using an [HttpClientEngine] and a base URL.
 *
 * @param engine The HTTP client engine to use for making network requests.
 * @param baseUrl The base URL for the API.
 * @param customHeaders Optional custom headers to include in the requests.
 */
internal class BroadcastService(
    engine: HttpClientEngine,
    baseUrl: String,
    customHeaders: Map<String, String> = emptyMap()
) : BaseService(engine, baseUrl, customHeaders), IBroadcastService {

    /**
     * Authenticates a broadcasting channel.
     */
    override suspend fun postAuth(
        socketId: String,
        channelName: String
    ): DataResult<BroadcastDTO.Response.Auth> {
        val data = mapOf(
            "socket_id" to socketId,
            "channel_name" to channelName
        )
        return postForm("broadcasting/auth", data) { it.serialize() }
    }
}
