package com.altrozdrive.app

/** Logical channels used only for classification/diagnostics; no guessed signal values. */
enum class LogicalChannel { STANDARD, NETWORK, CONTROL, SERVICE, VEHICLE_INFO, UNKNOWN }

data class ProtocolPacket(
    val sequence: Int,
    val messageType: Int,
    val body: ByteArray,
    val channel: LogicalChannel = LogicalChannel.STANDARD,
    val raw: ByteArray = body
)

data class DecodedSignal(val name: String, val value: Double, val signalId: Int? = null, val confidence: Double = 1.0)

/** Explicitly empty production decoder: packet-family-specific vehicle parsers are not yet proven. */
class VehiclePacketDecoderRegistry {
    fun decode(packet: ProtocolPacket): List<DecodedSignal> = emptyList()
}

class LogicalChannelRouter {
    fun route(packet: ProtocolPacket): ProtocolPacket = packet.copy(
        channel = when (packet.messageType) {
            ConnectNextHtsWire.NETWORK_ID -> LogicalChannel.NETWORK
            0x13, 0x14 -> LogicalChannel.CONTROL
            ConnectNextHtsWire.VEHICLE_SERVICE, ConnectNextHtsWire.VEHICLE_PACKET -> LogicalChannel.VEHICLE_INFO
            else -> LogicalChannel.UNKNOWN
        }
    )
}
