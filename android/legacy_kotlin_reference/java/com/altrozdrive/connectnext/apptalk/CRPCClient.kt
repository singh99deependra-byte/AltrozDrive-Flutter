package com.altrozdrive.connectnext.apptalk

class CRPCClient(private val server: CConnectionServer) {
    fun send(data: ByteArray) = server.publish(data)
}
