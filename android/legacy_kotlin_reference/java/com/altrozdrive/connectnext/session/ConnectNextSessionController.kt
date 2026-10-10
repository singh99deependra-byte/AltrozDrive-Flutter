package com.altrozdrive.connectnext.session

import com.altrozdrive.app.ConnectNextHtsReassembler
import com.altrozdrive.app.ConnectNextHtsWire
import com.altrozdrive.app.MirroredHtsFrame
import com.altrozdrive.connectnext.hts.remotelinkmgr.CPacketPacker
import com.altrozdrive.connectnext.hts.servicemgr.CServiceMgr
import java.util.concurrent.atomic.AtomicInteger

/** End-to-end protocol coordinator. Transport ownership remains with the Android Bluetooth layer. */
class ConnectNextSessionController(
    private val onFrame: (MirroredHtsFrame) -> Unit = {},
    private val onRaw: (ByteArray) -> Unit = {}
) {
    val services = CServiceMgr()
    val stateMachine = ConnectNextSessionStateMachine(services)
    private val reassembler = ConnectNextHtsReassembler()
    private val txSequence = AtomicInteger(0)
    private val packer = CPacketPacker()

    fun reset() { reassembler.reset(); txSequence.set(0); stateMachine.reset() }

    fun onRx(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        onRaw(bytes.copyOf())
        reassembler.append(bytes) { onFrame(it) }
    }

    fun onFrame(frame: MirroredHtsFrame) {
        stateMachine.accept(frame)
        onFrame.invoke(frame)
    }

    fun nextSequence(): Int = txSequence.getAndIncrement()
    fun networkIdentity(deviceName: String): ByteArray {
        val json = "{\"NETWORK_ID\":{\"PartNumberInfo\":{}},\"OWN_DEVICE_ID\":${ConnectNextHtsWire.CLIENT_ID},\"PEER_DEVICE_ID\":0,\"DEVICE_NAME\":\"${escapeJson(deviceName)}\",\"HTS_VERSION\":\"2.0\"}"
        val payload = json.toByteArray(Charsets.UTF_8)
        val body = ByteArray(7 + payload.size)
        val n = payload.size + 5
        body[0] = (n and 255).toByte(); body[1] = ((n ushr 8) and 255).toByte()
        body[2] = 1; body[3] = 0
        body[4] = ((payload.size ushr 16) and 255).toByte(); body[5] = ((payload.size ushr 8) and 255).toByte(); body[6] = (payload.size and 255).toByte()
        payload.copyInto(body, 7)
        return packer.pack(nextSequence(), ConnectNextHtsWire.NETWORK_ID, body, 0x3E)
    }

    private fun currentControlType(): Int = when (stateMachine.state.peerDeviceId) {
        19 -> 0x13
        20 -> 0x14
        else -> ConnectNextHtsWire.CONTROL_TEXT
    }

    fun controlText(text: String, trailer: Int): ByteArray = packer.pack(nextSequence(), currentControlType(), ConnectNextHtsWire.textBody(text), trailer)
    fun controlLength(payloadLength: Int): ByteArray = packer.pack(nextSequence(), currentControlType(), ConnectNextHtsWire.textLengthControl(payloadLength), 0x3E)
    @Suppress("DEPRECATION")
    fun ackFor(frame: MirroredHtsFrame): ByteArray? = ConnectNextHtsWire.let { wire ->
        val t = when {
            frame.trailer == 0xF8 -> 0x22
            frame.trailer == 0xE4 -> 0x3E
            frame.shortForm -> 0x3E
            else -> null
        }
        t?.let { wire.encodeAck(frame.sequence, it) }
    }

    private fun escapeJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
