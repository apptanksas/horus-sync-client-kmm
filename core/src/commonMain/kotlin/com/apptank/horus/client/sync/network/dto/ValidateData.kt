package com.apptank.horus.client.sync.network.dto

import kotlinx.serialization.Serializable


sealed class SyncDTO {
    @Serializable
    data class EntityHash(
        val entity: String,
        val hash: String
    )

    @Serializable
    data class EntityHashResponse(
        var entity: String? = null,
        var hash: HashValidation? = null
    )

    @Serializable
    data class HashValidation(
        var expected: String? = null,
        var obtained: String? = null,
        var matched: Boolean? = null
    )
}




