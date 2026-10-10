package com.altrozdrive.connectnext.connection.wifi

import java.io.Closeable

class WiFiConnection(val config: WiFiConnectionConfig) : Closeable {
    var connected: Boolean = false
        private set
    fun connect() { connected = true }
    override fun close() { connected = false }
}
