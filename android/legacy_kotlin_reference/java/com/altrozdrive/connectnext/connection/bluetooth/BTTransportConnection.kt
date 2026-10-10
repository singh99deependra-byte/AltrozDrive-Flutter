package com.altrozdrive.connectnext.connection.bluetooth

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

interface BTTransportConnection : Closeable {
    val input: InputStream
    val output: OutputStream
    fun isConnected(): Boolean
    override fun close()
}
