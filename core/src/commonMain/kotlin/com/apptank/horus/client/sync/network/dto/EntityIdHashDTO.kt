package com.apptank.horus.client.sync.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntityIdHashDTO(
    val id: String? = null,
    @SerialName("sync_hash") val hash: String? = null
)