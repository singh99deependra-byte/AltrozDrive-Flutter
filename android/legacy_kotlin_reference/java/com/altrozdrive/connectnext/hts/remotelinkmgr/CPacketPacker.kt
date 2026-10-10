package com.altrozdrive.connectnext.hts.remotelinkmgr

import com.altrozdrive.app.ConnectNextHtsWire

/** ConnectNext-style packet construction, using the empirically verified HTS codec. */
class CPacketPacker {
    fun pack(sequence: Int, type: Int, body: ByteArray, trailer: Int): ByteArray =
        ConnectNextHtsWire.encodeData(sequence, type, body, trailer)
    @Suppress("DEPRECATION")
    fun packAck(sequence: Int, trailer: Int): ByteArray =
        ConnectNextHtsWire.encodeAck(sequence, trailer)
}
