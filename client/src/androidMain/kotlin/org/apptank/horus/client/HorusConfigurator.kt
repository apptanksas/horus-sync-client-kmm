package org.apptank.horus.client

import android.content.Context
import org.apptank.horus.client.di.HorusContainer
import com.russhwolf.settings.SharedPreferencesSettings
import org.apptank.horus.client.config.HorusConfig
import java.io.File

/**
 * HorusConfigurator is a class that allows you to configure the Horus SDK.
 *
 * @param baseUrl The base URL of the API.
 */
class HorusConfigurator(
    private val config: HorusConfig
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
            setupConfig(config)
            setupDatabaseFactory(DatabaseDriverFactory(context))
            setupNetworkValidator(NetworkValidator(context))
            if (config.isDebug) setupLogger(AndroidLogger())
        }
    }
}