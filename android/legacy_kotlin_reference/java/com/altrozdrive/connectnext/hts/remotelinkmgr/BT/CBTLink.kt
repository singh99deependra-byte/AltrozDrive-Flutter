package com.altrozdrive.connectnext.hts.remotelinkmgr.BT

import com.altrozdrive.connectnext.connection.bluetooth.BTTransportConnection
import com.altrozdrive.connectnext.hts.remotelinkmgr.CLinkReader
import com.altrozdrive.connectnext.hts.remotelinkmgr.CRemoteLinkMgr
import java.util.concurrent.atomic.AtomicBoolean

class CBTLink(private val connection: BTTransportConnection) : AutoCloseable {
    val linkReader = CLinkReader({ frame -> onFrame?.invoke(CFrame(frame)) })
    val remoteLinkMgr = CRemoteLinkMgr(linkReader = linkReader)
    private val open = AtomicBoolean(false)
    private var reader: Thread? = null
    var onFrame: ((CFrame) -> Unit)? = null
    fun start() {
        if (!open.compareAndSet(false, true)) return
        reader = Thread {
            try {
                val buf = ByteArray(8192)
                while (open.get() && connection.isConnected()) {
                    val n = connection.input.read(buf)
                    if (n <= 0) break
                    linkReader.accept(buf.copyOf(n))
                }
            } finally { open.set(false) }
        }.apply { name = "ConnectNext-CBTLink"; start() }
    }
    override fun close() { open.set(false); try { connection.close() } catch (_: Throwable) {} }
}
