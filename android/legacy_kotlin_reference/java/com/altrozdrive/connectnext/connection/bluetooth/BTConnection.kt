package com.altrozdrive.connectnext.connection.bluetooth

import java.io.InputStream
import java.io.OutputStream

/** Clean-room transport facade mirroring ConnectNext's BTConnection responsibility. */
class BTConnection(
    private val transport: BTTransportConnection,
    val config: BTConnectionConfig = BTConnectionConfig()
) : BTTransportConnection {
    override val input: InputStream get() = transport.input
    override val output: OutputStream get() = transport.output
    override fun isConnected(): Boolean = transport.isConnected()
    override fun close() = transport.close()
}
