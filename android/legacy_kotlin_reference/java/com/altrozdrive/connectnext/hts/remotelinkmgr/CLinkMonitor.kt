package com.altrozdrive.connectnext.hts.remotelinkmgr

import java.util.concurrent.atomic.AtomicLong

class CLinkMonitor {
    private val rx = AtomicLong()
    private val tx = AtomicLong()
    fun onRx(bytes: Int) { rx.addAndGet(bytes.toLong()) }
    fun onTx(bytes: Int) { tx.addAndGet(bytes.toLong()) }
    fun rxBytes(): Long = rx.get()
    fun txBytes(): Long = tx.get()
}
