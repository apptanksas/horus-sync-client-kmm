package org.apptank.horus.client.restrictions

interface EntityRestriction {
    fun getEntityName(): String

    enum class OperationType {
        INSERT,
        READ,
        UPDATE,
        DELETE
    }
}