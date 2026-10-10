package com.altrozdrive.connectnext.connection

import java.util.concurrent.atomic.AtomicLong

/** Clean-room health/counters role observed in ConnectNext's connection package. */
class ConnectionHealthManager {
    private val rx = AtomicLong()
    private val tx = AtomicLong()
    private val errors = AtomicLong()
    fun onRx(bytes: Int) { if (bytes > 0) rx.addAndGet(bytes.toLong()) }
    fun onTx(bytes: Int) { if (bytes > 0) tx.addAndGet(bytes.toLong()) }
    fun onError() { errors.incrementAndGet() }
    fun rxBytes(): Long = rx.get()
    fun txBytes(): Long = tx.get()
    fun errorCount(): Long = errors.get()
    fun reset() { rx.set(0); tx.set(0); errors.set(0) }
}
