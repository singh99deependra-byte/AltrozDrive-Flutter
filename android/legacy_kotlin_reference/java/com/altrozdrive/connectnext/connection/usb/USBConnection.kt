package com.altrozdrive.connectnext.connection.usb

class USBConnection(val config: USBConnectionConfig = USBConnectionConfig()) : AutoCloseable {
    enum class State { IDLE, CONNECTING, CONNECTED, DISCONNECTED }
    var state: State = State.IDLE
        private set
    fun connect() { state = State.CONNECTING; state = State.CONNECTED }
    override fun close() { state = State.DISCONNECTED }
}
