package com.apptank.horus.client.sync.network.dto

import kotlinx.serialization.Serializable


data class EntityHash(
    val entity: String,
    val hash: String
)

@Serializable
data class EntityHashResponse(
    var entity: String? = null,
    var hashValidation: HashValidation? = null
)

@Serializable
data class HashValidation(
    var received: String? = null,
    var current: String? = null,
    var matched: Boolean? = null
)