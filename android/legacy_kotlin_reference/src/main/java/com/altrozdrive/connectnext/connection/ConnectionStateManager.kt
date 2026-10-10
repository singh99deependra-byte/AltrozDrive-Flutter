package com.altrozdrive.connectnext.connection

import java.util.concurrent.atomic.AtomicReference

/** Clean-room state holder corresponding to ConnectNext ConnectionStateManager. */
class ConnectionStateManager {
    enum class State { IDLE, CONNECTING, CONNECTED, CLOSING, DISCONNECTED, ERROR }
    private val current = AtomicReference(State.IDLE)
    @Volatile var lastError: Throwable? = null
        private set

    fun getState(): State = current.get()
    fun transition(next: State, error: Throwable? = null) {
        lastError = error
        current.set(next)
    }
    fun reset() {
        lastError = null
        current.set(State.IDLE)
    }
}
