package com.altrozdrive.connectnext.connection.interfaces

import com.altrozdrive.connectnext.connection.enums.ConnectionType

interface IConnectionListener {
    fun onConnected(type: ConnectionType)
    fun onDisconnected(type: ConnectionType, cause: Throwable? = null)
}
