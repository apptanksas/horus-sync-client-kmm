package com.apptank.horus.client.data

import com.apptank.horus.client.sync.network.dto.SyncDTO

internal fun List<Horus.EntityHash>.toDTORequest(): List<SyncDTO.Request.EntityHash> {
    return map { it.toDTORequest() }
}

internal fun Horus.EntityHash.toDTORequest(): SyncDTO.Request.EntityHash {
    return SyncDTO.Request.EntityHash(entity, hash)
}
