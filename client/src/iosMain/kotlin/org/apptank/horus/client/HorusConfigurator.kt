package org.apptank.horus.client

import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.INetworkValidator
import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

class HorusConfigurator(
    private val baseUrl: String,
    private val isDebug: Boolean = false
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
            setupBaseUrl(baseUrl)
            setupDatabaseFactory(DatabaseDriverFactory())
            setupNetworkValidator(networkValidator)
            if (isDebug) setupLogger(IOSLogger())
        }
    }
}