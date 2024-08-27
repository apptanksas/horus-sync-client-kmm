package com.apptank.horus.client.base

enum class SynchronizationStatus {
    // Service to validate hashing is in progress
    SYSTEM_SERVICE_VALIDATE_HASHING_IN_PROGRESS,

    // Service to validate hashing was successful
    SYSTEM_SERVICE_VALIDATE_HASHING_SUCCESS,

    // Service to validate hashing failed
    SYSTEM_SERVICE_VALIDATE_HASHING_FAILURE,

    // Service to get migration scheme is in progress
    MIGRATION_SERVICE_IN_PROGRESS,

    // Service to get migration scheme was successful
    MIGRATION_SERVICE_SUCCESS,

    // Service to get migration scheme failed
    MIGRATION_SERVICE_FAILURE,

    // Migration of database is in progress
    MIGRATION_DATABASE_IN_PROGRESS,

    // Migration of database was successful
    MIGRATION_DATABASE_SUCCESS,

    // Migration of database failed
    MIGRATION_DATABASE_FAILURE,

    // Service to get data is in progress
    SYNC_SERVICE_GET_DATA_IN_PROGRESS,

    // Service to get data was successful
    SYNC_SERVICE_GET_DATA_SUCCESS,

    // Service to get data failed
    SYNC_SERVICE_GET_DATA_FAILURE,

    // Database update is in progress
    SYNC_DATABASE_UPDATE_IN_PROGRESS,

    // Database update was successful
    SYNC_DATABASE_UPDATE_SUCCESS,

    // Database update failed
    SYNC_DATABASE_UPDATE_FAILURE,

    // Synchronization is good
    SYNC_OK,

    // Synchronization failed
    NETWORK_NOT_AVAILABLE,
}