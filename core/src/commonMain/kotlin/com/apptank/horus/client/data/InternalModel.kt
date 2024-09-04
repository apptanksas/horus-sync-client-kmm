package com.apptank.horus.client.data

sealed class InternalModel {
    data class EntityIdHash(
        val id: String,
        val hash: String
    )

    data class EntityHashValidation(
        val entity: String,
        val hashExpected: String,
        val hashObtained: String,
        val isHashMatched: Boolean
    )
}