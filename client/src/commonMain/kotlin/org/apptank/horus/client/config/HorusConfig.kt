package org.apptank.horus.client.config

import org.apptank.horus.client.sync.upload.data.FileMimeType

/**
 * The `HorusConfig` class holds the configuration settings for the Horus synchronization system.
 * It includes the base URL for the server, settings for handling pending actions, and an option
 * to enable or disable debug mode.
 *
 * @property baseUrl The base URL of the remote server used for synchronization.
 * @property uploadFilesConfig Configuration for uploading files to the server.
 * @property pushPendingActionsConfig Configuration for managing the batch size and expiration time for pending actions.
 * @property isDebug Flag to enable or disable debug mode. Default is `false`.
 */
data class HorusConfig(
    val baseUrl: String,
    val uploadFilesConfig: UploadFilesConfig,
    val pushPendingActionsConfig: PushPendingActionsConfig = PushPendingActionsConfig(),
    val isDebug: Boolean = false
)

/**
 * The `PushPendingActionsConfig` class defines the settings for managing pending actions before synchronization.
 * It includes the size of batches and the time expiration threshold to do synchronization.
 *
 * @property batchSize The number of pending actions required to trigger synchronization. Default is `10`.
 * @property expirationTime The maximum time in seconds allowed between synchronizations before forcing one.
 *                          Default is 12 hours .
 */
data class PushPendingActionsConfig(
    val batchSize: Int = 10,
    val expirationTime: Long = 60 * 60 * 12 // 12 hours
)

/**
 * The `UploadFilesConfig` class defines the settings for uploading files to the server.
 * It includes the base storage path, the list of allowed MIME types, and the maximum file size allowed.
 *
 * @property baseStoragePath The base path for storing files.
 * @property mimeTypesAllowed The list of allowed MIME types for file uploads.
 * @property maxFileSize The maximum file size allowed in bytes.
 */
data class UploadFilesConfig(
    val baseStoragePath: String,
    val mimeTypesAllowed: List<FileMimeType>,
    val maxFileSize: Int
)
