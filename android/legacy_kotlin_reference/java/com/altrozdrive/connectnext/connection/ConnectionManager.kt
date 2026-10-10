package com.altrozdrive.connectnext.connection

import com.altrozdrive.connectnext.connection.bluetooth.BTConnection
import com.altrozdrive.connectnext.connection.bluetooth.BTTransportConnection
import com.altrozdrive.connectnext.hts.remotelinkmgr.BT.CBTConnectionManager
import com.altrozdrive.connectnext.session.ConnectNextSessionController

/** Clean-room coordinator corresponding to ConnectNext ConnectionManager. */
class ConnectionManager(
    private val stateManager: ConnectionStateManager = ConnectionStateManager(),
    private val btConnectionManager: CBTConnectionManager = CBTConnectionManager(),
    private val sessionController: ConnectNextSessionController = ConnectNextSessionController()
) : AutoCloseable {
    fun state(): ConnectionStateManager.State = stateManager.getState()
    fun session(): ConnectNextSessionController = sessionController

    @Synchronized
    fun attach(transport: BTTransportConnection) {
        stateManager.transition(ConnectionStateManager.State.CONNECTING)
        try {
            val connection = BTConnection(transport)
            btConnectionManager.attach(connection)
            stateManager.transition(ConnectionStateManager.State.CONNECTED)
        } catch (t: Throwable) {
            stateManager.transition(ConnectionStateManager.State.ERROR, t)
            throw t
        }
    }

    override fun close() {
        stateManager.transition(ConnectionStateManager.State.CLOSING)
        try { btConnectionManager.close() }
        finally {
            sessionController.reset()
            stateManager.transition(ConnectionStateManager.State.DISCONNECTED)
        }
    }
}
