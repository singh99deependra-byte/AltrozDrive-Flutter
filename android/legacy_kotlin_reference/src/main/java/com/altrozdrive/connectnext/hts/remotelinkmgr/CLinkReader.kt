package com.altrozdrive.connectnext.hts.remotelinkmgr

import com.altrozdrive.app.ConnectNextHtsReassembler
import com.altrozdrive.app.MirroredHtsFrame

class CLinkReader(
    private val onFrame: (MirroredHtsFrame) -> Unit,
    private val onRaw: (ByteArray) -> Unit = {}
) {
    private val reassembler = ConnectNextHtsReassembler()
    fun accept(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        onRaw(bytes.copyOf())
        reassembler.append(bytes, onFrame)
    }
    fun reset() = reassembler.reset()
}
