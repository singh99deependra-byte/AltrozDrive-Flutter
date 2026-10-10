package com.altrozdrive.connectnext.vehicle.client.model.VO

import com.altrozdrive.connectnext.vehicle.client.utils.X451PacketRecord

data class VehiclePacketVO(
    val parserFamily: String,
    val raw: ByteArray,
    val values: Map<String, Double> = emptyMap(),
    val validated: Boolean = false,
    val periodMs: Int? = null,
    val packets: List<X451PacketRecord> = emptyList(),
    val cursorStart: Int = 0,
    val consumedBytes: Int = 0,
    val decodeComplete: Boolean = false,
    val decodeError: String? = null
)
