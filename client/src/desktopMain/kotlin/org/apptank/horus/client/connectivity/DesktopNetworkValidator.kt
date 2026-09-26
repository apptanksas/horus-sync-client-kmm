package org.apptank.horus.client.connectivity

import org.apptank.horus.client.base.Callback
import java.net.URI
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** Checks reachability of the configured server off the UI thread. */
class DesktopNetworkValidator(baseUrl: String) : INetworkValidator, AutoCloseable {
    private val uri = URI(baseUrl)
    private val callbacks = CopyOnWriteArrayList<Callback>()
    private val executor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "horus-connectivity").apply { isDaemon = true }
    }
    @Volatile private var available = false
    private var monitor: ScheduledFuture<*>? = null
    init {
        require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank()) { "Invalid server URL" }
        executor.execute { refresh() }
    }
    override fun isNetworkAvailable(): Boolean = getNetworkInfo().hasValidConnection()
    override fun getNetworkInfo(): Network = if (available) Network(listOf(
        NetworkConnection(ConnectionType.UNKNOWN, ConnectionLevel.HIGH)
    )) else Network.noConnections()
    override fun onNetworkChange(callback: Callback) { callbacks.addIfAbsent(callback) }
    @Synchronized override fun registerNetworkCallback() {
        if (monitor?.isCancelled == false) return
        monitor = executor.scheduleWithFixedDelay({ refresh() }, 0, 5, TimeUnit.SECONDS)
    }
    @Synchronized override fun unregisterNetworkCallback() {
        monitor?.cancel(false)
        monitor = null
    }
    fun removeNetworkCallback(callback: Callback) { callbacks.remove(callback) }
    private fun refresh() {
        val reachable = runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(uri.host,
                    if (uri.port >= 0) uri.port else if (uri.scheme == "https") 443 else 80), 2000)
            }
            true
        }.getOrDefault(false)
        if (reachable != available) {
            available = reachable
            callbacks.forEach { callback -> runCatching { callback() } }
        }
    }
    override fun close() {
        unregisterNetworkCallback()
        callbacks.clear()
        executor.shutdownNow()
    }
}
