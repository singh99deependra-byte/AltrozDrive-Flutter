package com.altrozdrive.connectnext.hts.remotelinkmgr.BT

import com.altrozdrive.app.MirroredHtsFrame

/** Immutable frame object corresponding to the role of ConnectNext CFrame. */
data class CFrame(val wire: MirroredHtsFrame) {
    val sequence: Int get() = wire.sequence
    val type: Int? get() = wire.messageType
    val body: ByteArray get() = wire.body.copyOf()
    val checksum: Int? get() = wire.checksum
    val checksumValid: Boolean get() = wire.checksumValid
    val shortForm: Boolean get() = wire.shortForm
    val trailer: Int? get() = wire.trailer
    val raw: ByteArray get() = wire.raw.copyOf()
}
