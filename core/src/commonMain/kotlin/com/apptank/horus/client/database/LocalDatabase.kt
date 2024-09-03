package com.apptank.horus.client.database

import kotlinx.datetime.LocalDateTime


sealed class LocalDatabase {

    /**
     * Entity record from database
     */
    data class EntityRecord(
        val id: String,
        val userId: Int,
        val hash: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
        private val attributes: Map<String, Any>
    ) {
        fun getInt(attr: String): Int {
            return attributes[attr] as? Int?
                ?: throw IllegalStateException("Attribute [$attr] is not an Integer")
        }

        fun getLong(attr: String): Long {
            return attributes[attr] as? Long?
                ?: throw IllegalStateException("Attribute [$attr] is not a Long")
        }

        fun getDouble(attr: String): Double {
            return attributes[attr] as? Double?
                ?: throw IllegalStateException("Attribute [$attr] is not a Double")
        }

        fun getString(attr: String): String {
            return attributes[attr] as? String?
                ?: throw IllegalStateException("Attribute [$attr] is not a String")
        }

        fun getBoolean(attr: String): Boolean {
            return attributes[attr] as? Boolean?
                ?: throw IllegalStateException("Attribute [$attr] is not a Boolean")
        }
    }


    /**
     * Data class representing the result of a database operation.
     *
     * @property isSuccess Indicates if the operation was successful.
     * @property rowsAffected The number of rows affected by the operation.
     * @property isFailure Indicates if the operation was failure.
     */
    data class OperationResult(
        val isSuccess: Boolean,
        val rowsAffected: Int,
        val isFailure: Boolean = !isSuccess,
    )


}
