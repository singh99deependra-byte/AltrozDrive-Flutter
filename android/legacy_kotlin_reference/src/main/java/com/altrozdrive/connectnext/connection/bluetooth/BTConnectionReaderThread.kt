package com.altrozdrive.connectnext.connection.bluetooth

import java.util.concurrent.atomic.AtomicBoolean

class BTConnectionReaderThread(
    private val connection: BTTransportConnection,
    private val onBytes: (ByteArray) -> Unit,
    private val onClosed: (Throwable?) -> Unit = {}
) : Thread("ConnectNext-BTConnectionReaderThread") {
    private val running = AtomicBoolean(true)
    override fun run() {
        var error: Throwable? = null
        val buffer = ByteArray(8192)
        try {
            while (running.get() && connection.isConnected()) {
                val n = connection.input.read(buffer)
                if (n < 0) break
                if (n > 0) onBytes(buffer.copyOf(n))
            }
        } catch (t: Throwable) {
            error = t
        } finally {
            running.set(false)
            onClosed(error)
        }
    }
    fun shutdown() { running.set(false); interrupt() }
}
