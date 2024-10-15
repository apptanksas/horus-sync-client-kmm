package org.apptank.horus.client.config

/**
 * The `HorusConfig` class holds the configuration settings for the Horus synchronization system.
 * It includes the base URL for the server, settings for handling pending actions, and an option
 * to enable or disable debug mode.
 *
 * @property baseUrl The base URL of the remote server used for synchronization.
 * @property baseStoragePath The base path for storing files.
 * @property pushPendingActionsConfig Configuration for managing the batch size and expiration time for pending actions.
 * @property isDebug Flag to enable or disable debug mode. Default is `false`.
 */
data class HorusConfig(
    val baseUrl: String,
    val baseStoragePath: String,
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
