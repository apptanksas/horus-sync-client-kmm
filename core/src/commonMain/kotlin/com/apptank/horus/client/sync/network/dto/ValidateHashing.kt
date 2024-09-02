package com.apptank.horus.client.sync.network.dto

import com.apptank.horus.client.base.DataMap
import kotlinx.serialization.Serializable

@Serializable
data class ValidateHashingRequest(
    val data: DataMap,
    val hash: String
)

@Serializable
data class ValidateHashingResponse(
    var received: String? = null,
    var obtained: String? = null,
    var matched: Boolean? = null
)