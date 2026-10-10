package com.altrozdrive.connectnext.hts.remotelinkmgr.USB

class CUSBLink {
    enum class eLinkState { IDLE, CONNECTING, CONNECTED, DISCONNECTED }
    @Volatile var state: eLinkState = eLinkState.IDLE
        private set
    fun open() { state = eLinkState.CONNECTED }
    fun close() { state = eLinkState.DISCONNECTED }
}
