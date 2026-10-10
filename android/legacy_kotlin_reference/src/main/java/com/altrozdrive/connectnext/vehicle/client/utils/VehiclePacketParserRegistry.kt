package com.altrozdrive.connectnext.vehicle.client.utils

import com.altrozdrive.connectnext.vehicle.client.model.VO.VehiclePacketVO

/**
 * Parser registry mirroring ConnectNext's family-based parser selection responsibility.
 * No parser is considered valid unless it positively identifies the raw packet.
 */
class VehiclePacketParserRegistry(
    private val parsers: List<AbsVehiclePacketParser> = listOf(
        VehiclePacketParser_HEXA(),
        VehiclePacketParser_X205(),
        VehiclePacketParser_X445(),
        VehiclePacketParser_X451(),
        VehiclePacketParser_X104(),
        VehiclePacketParser_X0()
    )
) {
    fun select(raw: ByteArray): AbsVehiclePacketParser? = parsers.firstOrNull { it.canParse(raw) }

    fun parse(raw: ByteArray): VehiclePacketVO {
        val parser = select(raw)
        return parser?.parse(raw) ?: VehiclePacketVO("UNKNOWN", raw.copyOf(), emptyMap(), false)
    }

    /**
     * Explicit ConnectNext variant-aware entry point. The official 6.40 app
     * selects X451 from the exact UID/variant string, then supplies the VIS
     * callback period separately to the parser.
     */
    fun parseForVariant(raw: ByteArray, variant: String, periodMs: Int): VehiclePacketVO {
        return if (variant.equals(VehiclePacketParser_X451.EXACT_UID_VARIANT, ignoreCase = true)) {
            VehiclePacketParser_X451().parse(raw, periodMs)
        } else {
            parse(raw)
        }
    }
}
