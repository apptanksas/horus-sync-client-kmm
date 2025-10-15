package org.apptank.horus.client.base.error

import kotlinx.serialization.Serializable
import org.apptank.horus.client.base.DataMap

@Serializable
data class ErrorRestrictionResponse(
    val message: String,
    val code: String,
    val context: DataMap
)

