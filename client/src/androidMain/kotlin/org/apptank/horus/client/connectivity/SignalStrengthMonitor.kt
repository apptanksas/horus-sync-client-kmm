package org.apptank.horus.client.connectivity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.*
import org.apptank.horus.client.base.CallbackNullable

/**
 * SignalStrengthMonitor
 *
 * A lightweight helper that monitors changes to the device's cellular signal
 * and telephony display information (for example: changes between 4G/5G, VoLTE flags, etc.).
 *
 * This class:
 *  - Uses the modern TelephonyCallback API on Android S (API 31) and above.
 *  - Falls back to PhoneStateListener on older OS versions.
 *  - Invokes a user-provided callback whenever relevant telephony events occur.
 *
 * Important notes:
 *  - This monitor **requires** runtime permission(s). At minimum, READ_PHONE_STATE must
 *    be granted for the monitor to register. On many devices / OS versions, ACCESS_FINE_LOCATION
 *    is also required to access cell-related data (cell info / signal strength).
 *  - If the required permission is not granted, `startListening()` returns early and does not
 *    register any listener — no exception is thrown.
 *  - TelephonyCallback callbacks are delivered on the Executor provided to
 *    `TelephonyManager.registerTelephonyCallback(...)`. In this implementation the
 *    `context.mainExecutor` is used, so TelephonyCallback results will be delivered on
 *    the main thread.
 *  - The PhoneStateListener fallback will receive callbacks on a binder/telephony thread;
 *    the `onSignalChanged` callback will therefore be invoked on that background thread.
 *    Consumers that update the UI must dispatch to the main thread when using the fallback.
 *  - Always call `stopListening()` when the monitor is no longer required to avoid leaks.
 *
 * Example:
 * ```
 * val monitor = SignalStrengthMonitor(context)
 * monitor.setOnSignalChangedListener {
 *     // React to signal/display changes (update UI, metrics, etc.)
 *     // Ensure UI updates are made on the main thread if you're not already on it.
 * }
 * monitor.startListening()
 *
 * // later...
 * monitor.stopListening()
 * ```
 *
 * Thread-safety:
 *  - The class is not strictly synchronized. Register/unregister should be done from
 *    a single thread (typically the main thread).
 *
 * @param context  a valid Context instance used to obtain TelephonyManager and mainExecutor.
 */
class SignalStrengthMonitor(
    private val context: Context,
) {
    private var telephonyCallback: TelephonyCallback? = null

    /**
     * A nullable zero-argument callback invoked whenever a relevant telephony event occurs.
     *
     * Expected type: (() -> Unit)? (i.e., a nullable function with no parameters).
     * The callback does not receive detailed data; it is intended as a lightweight
     * “something changed” notifier. If you need the actual SignalStrength object,
     * adapt this API to expose it.
     */
    private var onSignalChanged: CallbackNullable = null

    /**
     * Sets a listener that will be invoked when the signal/display information changes.
     * The listener can be set at any time before or after startListening().
     *
     * @param callback a nullable function that will be called on change events.
     */
    fun setOnSignalChangedListener(callback: CallbackNullable) {
        onSignalChanged = callback
    }

    /**
     * Starts monitoring telephony signal/display changes.
     *
     * Behavior:
     *  - If READ_PHONE_STATE permission is not granted, the method returns immediately.
     *  - On Android S+ (API 31+) it registers a TelephonyCallback delivered on context.mainExecutor.
     *  - On older releases it registers a PhoneStateListener fallback.
     *
     * This method is idempotent in the sense that calling it multiple times will re-register
     * a new listener. Prefer calling startListening() once (for example from Activity.onStart()).
     */
    fun startListening() {

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(),
                TelephonyCallback.SignalStrengthsListener,
                TelephonyCallback.DisplayInfoListener {

                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    // Delivered on context.mainExecutor (main thread) because we register with mainExecutor.
                    onSignalChanged?.invoke()
                }

                override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                    // Display info contains information about network type (e.g. LTE/NR), VoLTE, etc.
                    onSignalChanged?.invoke()
                }
            }

            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                telephonyCallback as TelephonyCallback
            )

        } else {
            // Deprecated on API 31+, but required for older devices.
            // Note: PhoneStateListener callbacks are invoked on the telephony binder thread.
            @Suppress("DEPRECATION")
            telephonyManager.listen(object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    super.onSignalStrengthsChanged(signalStrength)
                    // Because this runs on a binder thread, consumers must dispatch to the UI thread to update UI.
                    onSignalChanged?.invoke()
                }
            }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }
    }

    /**
     * Stops monitoring telephony changes and unregisters any previously registered callback.
     *
     * Safe to call multiple times. After calling this method the monitor will no longer
     * invoke the `onSignalChanged` callback.
     */
    fun stopListening() {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        }
        // For the PhoneStateListener fallback, the system automatically clears the listener
        // when the process ends; if you need explicit unregistration for older APIs you can
        // track the PhoneStateListener instance and call telephonyManager.listen(listener, LISTEN_NONE).
    }
}
