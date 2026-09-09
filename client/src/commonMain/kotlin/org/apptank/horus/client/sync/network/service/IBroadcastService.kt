package org.apptank.horus.client.sync.network.service

import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.sync.network.dto.BroadcastDTO

/**
 * Interface defining the broadcasting service operations.
 */
interface IBroadcastService {

    /**
     * Authenticates a broadcasting channel.
     *
     * @param socketId The unique socket ID of the connection.
     * @param channelName The name of the channel to authenticate.
     * @return [DataResult] containing [BroadcastDTO.Response.Auth] if successful.
     */
    suspend fun postAuth(socketId: String, channelName: String): DataResult<BroadcastDTO.Response.Auth>
}
