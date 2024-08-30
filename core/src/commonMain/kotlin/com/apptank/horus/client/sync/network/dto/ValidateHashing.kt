package com.apptank.horus.client.sync.network.dto

import kotlinx.serialization.Serializable

data class ValidateHashingRequest(
    val data: MutableMap<String, Any>,
    val hash: String
)

@Serializable
data class ValidateHashingResponse(
    var received: String? = null,
    var obtained: String? = null,
    var matched: Boolean? = null
)