package com.apptank.horus.client

import android.content.Context
import com.apptank.horus.client.di.HorusContainer
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * HorusConfigurator is a class that allows you to configure the Horus SDK.
 *
 * @param baseUrl The base URL of the API.
 */
class HorusConfigurator(
    private val baseUrl: String
) {

    /**
     * Configures the Horus SDK with the given context.
     *
     * @param context The context to use for configuration.
     */
    fun configure(context: Context) {
        with(HorusContainer) {
            setupSettings(
                SharedPreferencesSettings(
                    context.getSharedPreferences(
                        "horus_settings", Context.MODE_PRIVATE
                    )
                )
            )
            setupBaseUrl(baseUrl)
            setupDatabaseFactory(DatabaseDriverFactory(context))
            setupNetworkValidator(NetworkValidator(context))
        }
    }
}