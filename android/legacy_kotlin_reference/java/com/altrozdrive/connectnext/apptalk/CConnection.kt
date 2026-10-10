package com.altrozdrive.connectnext.apptalk

class CConnection(val info: CConnectionInfo) {
    @Volatile var open: Boolean = true
    fun close() { open = false }
}
