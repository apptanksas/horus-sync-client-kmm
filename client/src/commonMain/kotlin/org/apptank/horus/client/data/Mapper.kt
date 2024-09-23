package org.apptank.horus.client.data

import org.apptank.horus.client.sync.network.dto.SyncDTO

/**
 * Extension function to convert a list of [Horus.EntityHash] objects to a list of [SyncDTO.Request.EntityHash] objects.
 *
 * @return A list of [SyncDTO.Request.EntityHash] objects.
 */
internal fun List<Horus.EntityHash>.toDTORequest(): List<SyncDTO.Request.EntityHash> {
    return map { it.toDTORequest() }
}

/**
 * Extension function to convert a [Horus.EntityHash] object to a [SyncDTO.Request.EntityHash] object.
 *
 * @return A [SyncDTO.Request.EntityHash] object.
 */
internal fun Horus.EntityHash.toDTORequest(): SyncDTO.Request.EntityHash {
    return SyncDTO.Request.EntityHash(entity, hash)
}
