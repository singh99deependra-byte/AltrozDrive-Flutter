package com.altrozdrive.connectnext.connection.androidauto

class AndroidAutoConnectionService {
    private var connection: AndroidAutoConnection? = null
    fun start(): AndroidAutoConnection = AndroidAutoConnection().also { connection = it; it.markConnected() }
    fun stop() { connection?.close(); connection = null }
    fun current(): AndroidAutoConnection? = connection
}
