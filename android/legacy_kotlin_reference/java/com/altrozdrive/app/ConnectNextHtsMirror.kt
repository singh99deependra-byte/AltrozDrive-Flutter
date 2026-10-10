package com.altrozdrive.app

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream

/**
 * Clean-room HTS wire model derived only from the supplied ConnectNext 6.40
 * HCI capture. It deliberately contains no vehicle-value guessing.
 */
data class MirroredHtsFrame(
    val directionType: Int,
    val sequence: Int,
    val messageType: Int? = null,
    val body: ByteArray = byteArrayOf(),
    val checksum: Int? = null,
    val checksumValid: Boolean = true,
    val shortForm: Boolean = false,
    val trailer: Int? = null,
    val raw: ByteArray
)

object ConnectNextHtsWire {
    const val DATA = 0x55
    const val ACK = 0xAA
    const val CONTROL_TEXT = 0x14
    const val VEHICLE_SERVICE = 0x1B
    const val VEHICLE_PACKET = 0x15
    /** Live 6.40/X451 VehicleInfo payload type observed in the user's HU session. */
    const val VEHICLE_PACKET_X451 = 0x25
    /**
     * Live VehicleInfo stream type observed after reconnects in the 2026-09-21
     * capture. Its 614-byte body is the same X451 signal layout as 0x25.
     */
    const val VEHICLE_PACKET_X451_LIVE = 0x2F
    const val NETWORK_ID = 0x00
    const val CLIENT_ID = 20

    /**
     * Official ConnectNext CFrame-compatible checksum.
     * Java/Kotlin byte arithmetic is signed; preserve that behavior and keep
     * the full 32-bit accumulator. The wire value is LE32.
     */
    fun additiveChecksum32(rawFrom55ThroughBody: ByteArray): Int {
        var sum = 0
        for (i in 1 until rawFrom55ThroughBody.size) sum += rawFrom55ThroughBody[i].toInt()
        return sum
    }

    @Deprecated("Use additiveChecksum32; retained for old tests only")
    fun additiveChecksum(rawTypeAndBody: ByteArray): Int = additiveChecksum32(rawTypeAndBody)

    fun encodeData(sequence: Int, messageType: Int, body: ByteArray, trailer: Int? = null): ByteArray {
        require(messageType in 0..255)
        require(body.size <= 0xFFFF - 1)
        val out = java.io.ByteArrayOutputStream(14 + body.size)
        out.write(0x7E); out.write(DATA)
        writeLe32(out, sequence)
        val frameLen = body.size + 1
        writeLe16(out, frameLen)
        out.write(messageType)
        out.write(body)
        val partial = out.toByteArray()
        writeLe32(out, additiveChecksum32(partial))
        out.write(0x7C)
        // The byte seen after 0x7C in the HCI capture is the lower RFCOMM FCS;
        // BluetoothSocket exposes only the application payload, so it is not emitted here.
        // trailer is retained only for legacy callers.
        if (trailer != null) { /* intentionally ignored */ }
        return stuffForWire(out.toByteArray())
    }

    /**
     * Phone-originated HTS ACK observed in the official session.
     * Layout: 7E AA | sequence LE32 | 00 00 | checksum LE32 | 7C.
     * The bytes that looked like `AA FF FF FF` in the capture are the signed-byte
     * 32-bit additive checksum for the ACK header, not an application trailer.
     */
    fun encodeAck(sequence: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(13)
        out.write(0x7E); out.write(ACK)
        writeLe32(out, sequence)
        writeLe16(out, 0)
        val partial = out.toByteArray()
        writeLe32(out, additiveChecksum32(partial))
        out.write(0x7C)
        return stuffForWire(out.toByteArray())
    }

    @Deprecated("Use encodeAck(sequence)")
    fun encodeAck(sequence: Int, @Suppress("UNUSED_PARAMETER") trailer: Int): ByteArray = encodeAck(sequence)

    // ------------------------------------------------------------------
    // HTS byte stuffing (wire form is 0x7E ... 0x7C)
    //
    // Evidence: the 2026-09-21 logs contain 53 escaped bytes, every one of them
    // `7D 7C` or `7D 7E`, and they occur ANYWHERE in the frame (header, body and
    // checksum). A logical 0x7C/0x7E is sent as 0x7D followed by that byte. A
    // bare 0x7D is NOT escaped (`7D 00` appears in every VehicleInfo frame and the
    // NETWORK_ID JSON contains a literal `7D 7D`).
    //
    // The old reassembler only un-stuffed the 8-byte header. A single escaped
    // byte in a body/checksum made the frame fail its length/checksum test, so it
    // was never ACKed, the HU re-sent the same sequence every 5 s and stopped
    // sending new vehicle data.
    // ------------------------------------------------------------------
    private const val MAX_FRAME_LEN = 16 * 1024

    /** Escape every 0x7C/0x7E strictly between the start (0x7E) and end (0x7C) delimiters. */
    fun stuffForWire(logical: ByteArray): ByteArray {
        if (logical.size < 3) return logical
        var extra = 0
        for (i in 1 until logical.size - 1) {
            val b = logical[i].toInt() and 0xFF
            if (b == 0x7C || b == 0x7E) extra++
        }
        if (extra == 0) return logical
        val out = ByteArray(logical.size + extra)
        var o = 0
        for (i in logical.indices) {
            val b = logical[i].toInt() and 0xFF
            if (i > 0 && i < logical.size - 1 && (b == 0x7C || b == 0x7E)) out[o++] = 0x7D.toByte()
            out[o++] = logical[i]
        }
        return out
    }

    sealed class WireParse {
        /** The wire bytes end before the frame does. Keep the buffer and wait. */
        object NeedMore : WireParse()
        /** Not a valid frame at this position; resynchronise on the next 0x7E. */
        object Invalid : WireParse()
        class Parsed(val frame: MirroredHtsFrame, val wireLength: Int) : WireParse()
    }

    /** Escape handling variants; the additive checksum decides which one is right. */
    private const val MODE_UNSTUFF = 0            // normal: collapse 7D7C / 7D7E everywhere
    private const val MODE_VERBATIM = 1           // no collapsing at all
    private const val MODE_LITERAL_7D_BEFORE_END = 2 // like 0: but last checksum byte is literal (e.g. 7D, then end 7C)

    private class WireCursor(private val data: ByteArray, var pos: Int, private val unstuff: Boolean) {
        /** Next logical byte (0..255), or -1 when the wire data ends first. */
        fun next(literal: Boolean = false): Int {
            if (pos >= data.size) return -1
            val b = data[pos].toInt() and 0xFF
            if (unstuff && !literal && b == 0x7D) {
                if (pos + 1 >= data.size) return -1
                val n = data[pos + 1].toInt() and 0xFF
                if (n == 0x7C || n == 0x7E) { pos += 2; return n }
            }
            pos++
            return b
        }
    }

    /**
     * Parses ONE frame that starts at data[0] == 0x7E, handling byte stuffing
     * anywhere in the frame. Never throws.
     */
    fun parseWireFrame(data: ByteArray): WireParse {
        if (data.isEmpty() || (data[0].toInt() and 0xFF) != 0x7E) return WireParse.Invalid
        var needMore = false
        for (mode in intArrayOf(MODE_UNSTUFF, MODE_VERBATIM, MODE_LITERAL_7D_BEFORE_END)) {
            when (val r = parseWireFrame(data, mode)) {
                is WireParse.Parsed -> return r
                is WireParse.NeedMore -> needMore = true
                is WireParse.Invalid -> Unit
            }
        }
        return if (needMore) WireParse.NeedMore else WireParse.Invalid
    }

    private fun parseWireFrame(data: ByteArray, mode: Int): WireParse {
        val cur = WireCursor(data, 1, mode != MODE_VERBATIM)
        val kind = cur.next()
        if (kind < 0) return WireParse.NeedMore

        if (kind == ACK) {
            val ack = ByteArray(13)
            ack[0] = 0x7E.toByte(); ack[1] = ACK.toByte()
            for (i in 2..12) {
                val literal = i == 12 || (mode == MODE_LITERAL_7D_BEFORE_END && i == 11)
                val b = cur.next(literal)
                if (b < 0) return WireParse.NeedMore
                ack[i] = b.toByte()
            }
            if ((ack[12].toInt() and 0xFF) != 0x7C) return WireParse.Invalid
            val checksum = le32(ack, 8)
            if (checksum != additiveChecksum32(ack.copyOfRange(0, 8))) return WireParse.Invalid
            return WireParse.Parsed(
                MirroredHtsFrame(
                    directionType = ACK,
                    sequence = le32(ack, 2),
                    checksum = checksum,
                    checksumValid = true,
                    raw = data.copyOfRange(0, cur.pos)
                ),
                cur.pos
            )
        }
        if (kind != DATA) return WireParse.Invalid

        val logical = ByteArray(8)
        logical[0] = 0x7E.toByte(); logical[1] = DATA.toByte()
        for (i in 2..7) {
            val b = cur.next()
            if (b < 0) return WireParse.NeedMore
            logical[i] = b.toByte()
        }
        val frameLen = le16(logical, 6)
        if (frameLen < 1 || frameLen > MAX_FRAME_LEN) return WireParse.Invalid

        val total = 8 + frameLen + 5
        val full = logical.copyOf(total)
        for (i in 8 until total) {
            val literal = i == total - 1 || (mode == MODE_LITERAL_7D_BEFORE_END && i == total - 2)
            val b = cur.next(literal)
            if (b < 0) return WireParse.NeedMore
            full[i] = b.toByte()
        }
        if ((full[total - 1].toInt() and 0xFF) != 0x7C) return WireParse.Invalid
        val bodyEnd = total - 5
        val checksum = le32(full, bodyEnd)
        if (checksum != additiveChecksum32(full.copyOfRange(0, bodyEnd))) return WireParse.Invalid
        return WireParse.Parsed(
            MirroredHtsFrame(
                directionType = DATA,
                sequence = le32(full, 2),
                messageType = full[8].toInt() and 0xFF,
                body = full.copyOfRange(9, bodyEnd),
                checksum = checksum,
                checksumValid = true,
                shortForm = false,
                trailer = null,
                raw = data.copyOfRange(0, cur.pos)
            ),
            cur.pos
        )
    }

    fun decode(raw: ByteArray): MirroredHtsFrame? {
        if (raw.size < 2 || (raw[0].toInt() and 0xFF) != 0x7E) return null
        val kind = raw[1].toInt() and 0xFF
        if (kind == ACK) {
            if (raw.size != 13 || (raw[12].toInt() and 0xFF) != 0x7C) return null
            val checksum = le32(raw, 8)
            val expected = additiveChecksum32(raw.copyOfRange(0, 8))
            return MirroredHtsFrame(
                directionType = kind,
                sequence = le32(raw, 2),
                checksum = checksum,
                checksumValid = checksum == expected,
                raw = raw.copyOf()
            )
        }
        if (kind != DATA || raw.size < 14) return null

        // Normal DATA frames are fixed-header/direct frames. Keep this path first
        // because ordinary JSON may legitimately contain 0x7D 0x7D and must not
        // be mistaken for an escape sequence.
        if (raw.size >= 8) {
            val frameLen = le16(raw, 6)
            decodeDirectDataFrame(raw, frameLen)?.let { return it }
        }

        // The HU also stuffs the DATA frame header itself. The 10:36:19 capture
        // is the concrete example: sequence 124 is transmitted as 7D 7C, so the
        // old decoder read bytes 6/7 from the wrong positions and could no longer
        // find the frame. Normalize from byte 1, not byte 8, so sequence, length,
        // message type, body and checksum all return to their logical positions.
        val logical = unstuffCompleteDataFrame(raw) ?: return null
        if (logical.size < 14 || (logical[1].toInt() and 0xFF) != DATA) return null

        val frameLen = le16(logical, 6)
        val bodyLen = frameLen - 1
        if (bodyLen < 0) return null
        val bodyStart = 9
        val bodyEnd = bodyStart + bodyLen
        if (logical.size != bodyEnd + 5) return null
        if ((logical[logical.size - 1].toInt() and 0xFF) != 0x7C) return null

        val checksum = le32(logical, bodyEnd)
        val expected = additiveChecksum32(logical.copyOfRange(0, bodyEnd))
        return MirroredHtsFrame(
            directionType = DATA,
            sequence = le32(logical, 2),
            messageType = logical[8].toInt() and 0xFF,
            body = logical.copyOfRange(bodyStart, bodyEnd),
            checksum = checksum,
            checksumValid = checksum == expected,
            shortForm = false,
            trailer = null,
            raw = raw.copyOf()
        ).takeIf { it.checksumValid }
    }

    private fun decodeDirectDataFrame(raw: ByteArray, frameLen: Int): MirroredHtsFrame? {
        val expectedSize = 8 + frameLen + 5
        if (frameLen < 1 || raw.size != expectedSize) return null
        if ((raw[0].toInt() and 0xFF) != 0x7E || (raw[1].toInt() and 0xFF) != DATA) return null
        val bodyLen = frameLen - 1
        val bodyStart = 9
        val bodyEnd = bodyStart + bodyLen
        if (bodyEnd + 5 != raw.size) return null
        if ((raw[raw.size - 1].toInt() and 0xFF) != 0x7C) return null
        val checksum = le32(raw, bodyEnd)
        val expected = additiveChecksum32(raw.copyOfRange(0, bodyEnd))
        return MirroredHtsFrame(
            directionType = DATA,
            sequence = le32(raw, 2),
            messageType = raw[8].toInt() and 0xFF,
            body = raw.copyOfRange(bodyStart, bodyEnd),
            checksum = checksum,
            checksumValid = checksum == expected,
            shortForm = false,
            trailer = null,
            raw = raw.copyOf()
        ).takeIf { it.checksumValid }
    }

    /**
     * Normalize one complete DATA frame when the fixed header contains HTS
     * byte-stuffing. The observed HU captures require header normalization
     * while preserving the DATA bytes verbatim.
     */
    private fun unstuffCompleteDataFrame(raw: ByteArray): ByteArray? {
        if (raw.size < 9 || (raw[0].toInt() and 0xFF) != 0x7E) return null
        val logical = ByteArrayOutputStream(minOf(raw.size, maxOf(14, raw.size)))
        logical.write(0x7E)
        var i = 1

        fun appendLogicalByte(): Boolean {
            if (i >= raw.size) return false
            val b = raw[i].toInt() and 0xFF
            if (b == 0x7D && i + 1 < raw.size) {
                val next = raw[i + 1].toInt() and 0xFF
                if (next == 0x7E || next == 0x7C || next == 0x7D) {
                    logical.write(next)
                    i += 2
                    return true
                }
            }
            logical.write(b)
            i++
            return true
        }

        // Header through frame length. We need eight logical bytes before the
        // declared logical DATA length can be trusted.
        while (logical.size() < 8) {
            if (!appendLogicalByte()) return null
        }
        if ((logical.toByteArray()[1].toInt() and 0xFF) != DATA) return null
        val frameLen = le16(logical.toByteArray(), 6)
        if (frameLen < 1 || frameLen > 0xFFFF - 1) return null
        val targetSize = 8 + frameLen + 5

        // The captured HU frames that trigger this path have stuffing in the
        // header (sequence/length area), while their DATA body is carried at
        // the declared logical length. Do not globally collapse 0x7D 0x7D in
        // the body: Network-ID JSON legitimately contains literal `}}`.
        val bodyAndTrailerBytes = targetSize - logical.size()
        if (bodyAndTrailerBytes < 0 || i + bodyAndTrailerBytes > raw.size) return null
        logical.write(raw, i, bodyAndTrailerBytes)
        i += bodyAndTrailerBytes
        if (logical.size() != targetSize) return null
        return logical.toByteArray()
    }

    /**
     * Official phone-originated NETWORK_ID body observed in ConnectNext 6.40.
     * Layout: [LE16 innerLength][01 00][3-byte BE JSON length][JSON].
     * The 3-byte length is protocol-specific and is NOT a normal LE32 field.
     */
    fun networkIdentityBody(jsonPayload: ByteArray): ByteArray {
        require(jsonPayload.size <= 0xFFFFFF) { "NETWORK_ID JSON too large: ${jsonPayload.size}" }
        val innerLength = 5 + jsonPayload.size
        return java.io.ByteArrayOutputStream(jsonPayload.size + 7).apply {
            write(innerLength and 0xFF)
            write((innerLength ushr 8) and 0xFF)
            write(0x01)
            write(0x00)
            write((jsonPayload.size ushr 16) and 0xFF)
            write((jsonPayload.size ushr 8) and 0xFF)
            write(jsonPayload.size and 0xFF)
            write(jsonPayload)
        }.toByteArray()
    }

    fun textBody(text: String): ByteArray {
        val payload = text.toByteArray(Charsets.UTF_8) + byteArrayOf(0)
        val out = java.io.ByteArrayOutputStream(payload.size + 2)
        writeLe16(out, payload.size)
        out.write(payload)
        return out.toByteArray()
    }

    fun textLengthControl(payloadLength: Int): ByteArray = byteArrayOf(
        0x04, 0x00, 0x00, 0x00, 0x00, (payloadLength and 0xFF).toByte()
    )

    private fun writeLe16(out: java.io.ByteArrayOutputStream, v: Int) { out.write(v and 0xFF); out.write((v ushr 8) and 0xFF) }
    private fun writeLe32(out: java.io.ByteArrayOutputStream, v: Int) { repeat(4) { i -> out.write((v ushr (8 * i)) and 0xFF) } }
    private fun le16(b: ByteArray, o: Int): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
    private fun le32(b: ByteArray, o: Int): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
}
class ConnectNextHtsReassembler {
    private val buffer = java.io.ByteArrayOutputStream()
    private val maxBuffer = 256 * 1024

    fun append(bytes: ByteArray, onFrame: (MirroredHtsFrame) -> Unit) {
        if (bytes.isEmpty()) return
        if (buffer.size() + bytes.size > maxBuffer) {
            val existing = buffer.toByteArray()
            val keep = minOf(existing.size, 4096)
            buffer.reset()
            if (keep > 0) buffer.write(existing.copyOfRange(existing.size - keep, existing.size))
        }
        buffer.write(bytes)
        var data = buffer.toByteArray()
        while (true) {
            val start = data.indexOfFirst { (it.toInt() and 0xFF) == 0x7E }
            if (start < 0) { buffer.reset(); return }
            if (start > 0) data = data.copyOfRange(start, data.size)

            // One stuffing-aware parser for ACK and DATA frames. It validates the
            // terminator and the additive checksum, so a byte-stuffed body/checksum
            // (7D 7C / 7D 7E) is decoded instead of silently dropped.
            when (val r = ConnectNextHtsWire.parseWireFrame(data)) {
                is ConnectNextHtsWire.WireParse.NeedMore -> { retain(data); return }
                is ConnectNextHtsWire.WireParse.Invalid -> data = data.copyOfRange(1, data.size)
                is ConnectNextHtsWire.WireParse.Parsed -> {
                    onFrame(r.frame)
                    data = data.copyOfRange(r.wireLength, data.size)
                    if (data.isEmpty()) { buffer.reset(); return }
                }
            }
        }
    }

    fun reset() { buffer.reset() }
    private fun retain(data: ByteArray) { buffer.reset(); buffer.write(data) }
}

enum class ConnectNextMessageKind { NETWORK_ID, CONTROL_TEXT, VEHICLE_BINARY, ACK, UNKNOWN }

object ConnectNextMessageClassifier {
    fun classify(frame: MirroredHtsFrame): ConnectNextMessageKind {
        if (frame.directionType == ConnectNextHtsWire.ACK) return ConnectNextMessageKind.ACK
        return when (frame.messageType) {
            ConnectNextHtsWire.NETWORK_ID -> ConnectNextMessageKind.NETWORK_ID
            0x13, 0x14 -> ConnectNextMessageKind.CONTROL_TEXT
            ConnectNextHtsWire.VEHICLE_SERVICE,
            ConnectNextHtsWire.VEHICLE_PACKET,
            ConnectNextHtsWire.VEHICLE_PACKET_X451,
            ConnectNextHtsWire.VEHICLE_PACKET_X451_LIVE -> ConnectNextMessageKind.VEHICLE_BINARY
            else -> ConnectNextMessageKind.UNKNOWN
        }
    }
}
