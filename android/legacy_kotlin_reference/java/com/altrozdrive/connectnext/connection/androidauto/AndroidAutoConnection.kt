package com.altrozdrive.connectnext.connection.androidauto

import java.io.Closeable

/** Android Auto transport role; intentionally not selected for the Bluetooth HTS path. */
class AndroidAutoConnection : Closeable {
    var connected: Boolean = false
        private set
    fun markConnected() { connected = true }
    override fun close() { connected = false }
}
