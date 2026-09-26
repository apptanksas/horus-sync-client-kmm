package org.apptank.horus.client.connectivity

enum class ConnectionType {
    WIFI,
    CELLULAR,
    /** Desktop connectivity whose transport cannot be classified by the host. */
    UNKNOWN,
}