package com.altrozdrive.connectnext.apptalk

class CConnectionServer(private val server: MessageServer = MessageServer()) {
    fun publish(data: ByteArray) = server.publish(data)
    fun messages(): MessageServer = server
}
