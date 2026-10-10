package com.altrozdrive.connectnext.vehicle.client.utils

/**
 * One packet produced by ConnectNext's VehiclePacketParser_X451.
 *
 * `fieldValues` are the raw values produced by the official parser before the
 * Signal Master applies datatype/scale/offset semantics. They are intentionally
 * not presented as vehicle engineering values yet.
 */
data class X451PacketRecord(
    val packetName: String,
    val periodMs: Int,
    val formatCode: Int,
    val fieldValues: List<Long>,
    val msgAvailable: Long,
    val eventPktChanged: Long? = null,
    val startOffset: Int,
    val endOffset: Int
)

data class X451DecodeResult(
    val periodMs: Int,
    val packets: List<X451PacketRecord>,
    val consumedBytes: Int,
    val complete: Boolean,
    val error: String? = null
)
