package org.apptank.horus.client

import com.russhwolf.settings.PreferencesSettings
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.ILogger
import java.nio.file.Path
import java.util.prefs.Preferences

/** The desktop host supplies connectivity, storage location and lifecycle events. */
class HorusConfigurator(private val config: HorusConfig) {
    fun configure(networkValidator: INetworkValidator, databaseDirectory: Path,
                  logger: ILogger? = null): DatabaseDriverFactory {
        val factory = DatabaseDriverFactory(databaseDirectory)
        with(HorusContainer) {
            setupSettings(PreferencesSettings(Preferences.userRoot().node("org/apptank/horus/client")))
            setupConfig(config)
            setupDatabaseFactory(factory)
            setupNetworkValidator(networkValidator)
            if (logger != null) setupLogger(logger)
        }
        return factory
    }
}
