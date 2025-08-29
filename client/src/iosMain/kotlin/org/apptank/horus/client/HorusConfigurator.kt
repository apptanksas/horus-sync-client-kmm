package org.apptank.horus.client

import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.connectivity.INetworkValidator
import com.russhwolf.settings.NSUserDefaultsSettings
import org.apptank.horus.client.config.HorusConfig
import platform.Foundation.NSUserDefaults

class HorusConfigurator(
   private val config: HorusConfig
) {

    /**
     * Configures the Horus SDK with the given context.
     *
     * @param context The context to use for configuration.
     */
    fun configure(networkValidator: INetworkValidator) {
        with(HorusContainer) {
            setupSettings(
                NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
            )
            setupConfig(config)
            setupDatabaseFactory(DatabaseDriverFactory())
            setupNetworkValidator(networkValidator)
            if (config.isDebug) setupLogger(IOSLogger())
        }
    }
}