package com.altrozdrive.connectnext.hts.remotelinkmgr.BT

import com.altrozdrive.connectnext.connection.bluetooth.BTTransportConnection
import java.util.concurrent.atomic.AtomicReference

class CBTConnectionManager {
    enum class State { IDLE, CONNECTING, CONNECTED, DISCONNECTED }
    private val current = AtomicReference<CBTLink?>(null)
    @Volatile var state: State = State.IDLE
        private set
    fun attach(connection: BTTransportConnection): CBTLink {
        current.getAndSet(null)?.close()
        state = State.CONNECTING
        return CBTLink(connection).also { current.set(it); state = State.CONNECTED; it.start() }
    }
    fun getCurrent(): CBTLink? = current.get()
    fun close() { current.getAndSet(null)?.close(); state = State.DISCONNECTED }
}
