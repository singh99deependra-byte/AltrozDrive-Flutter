package com.altrozdrive.connectnext.vehicle.client.utils

import com.altrozdrive.connectnext.vehicle.client.model.VO.VehiclePacketVO

/**
 * VehiclePacketParser_X451 is selected by ConnectNext for this HU's exact
 * UID/variant string: 544216271207.
 *
 * The no-period parse() intentionally remains evidence-only because the official
 * parser receives the VIS callback period separately. Call parse(raw, periodMs)
 * when that callback period is known.
 */
class VehiclePacketParser_X451 : AbsVehiclePacketParser("X451") {
    override fun canParse(raw: ByteArray): Boolean = false

    override fun parse(raw: ByteArray): VehiclePacketVO = raw(raw)

    fun parse(raw: ByteArray, periodMs: Int, cursorStart: Int = 0): VehiclePacketVO {
        val decoded = X451PacketCodec.decode(raw, periodMs, cursorStart)
        return VehiclePacketVO(
            parserFamily = family,
            raw = raw.copyOf(),
            values = emptyMap(),
            validated = decoded.complete,
            periodMs = periodMs,
            packets = decoded.packets,
            cursorStart = cursorStart,
            consumedBytes = decoded.consumedBytes,
            decodeComplete = decoded.complete,
            decodeError = decoded.error
        )
    }

    companion object {
        const val EXACT_UID_VARIANT = "544216271207"
    }
}
