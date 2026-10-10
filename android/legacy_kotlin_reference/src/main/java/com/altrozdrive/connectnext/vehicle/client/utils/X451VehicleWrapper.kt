package com.altrozdrive.connectnext.vehicle.client.utils

/**
 * Evidence-only wrapper handling for the live X451/VehicleInfo payload.
 *
 * The captured 5.9.8.5 sessions show a small wrapper around the vehicle data:
 *   [LE16 declaredInnerLength][LE16 flags=0][payload]
 * where declaredInnerLength == raw.size - 2.
 *
 * This class ONLY validates/removes that wrapper. It does not decode vehicle
 * signals, apply Signal Master metadata, choose a callback period, or mutate
 * the transport/session state.
 */
object X451VehicleWrapper {
    data class Unwrapped(
        val rawSize: Int,
        /** The LE16 value seen on the wire; for captures this equals rawSize - 2. */
        val declaredInnerLength: Int,
        val headerSize: Int,
        val payload: ByteArray,
        val flags: Int
    )

    fun unwrap(raw: ByteArray): Unwrapped? {
        if (raw.size < 4) return null
        val declared = le16(raw, 0)
        val flags = le16(raw, 2)
        if (flags != 0) return null
        if (declared != raw.size - 2) return null
        val payloadLength = raw.size - 4
        if (payloadLength < 0 || declared != payloadLength + 2) return null
        return Unwrapped(
            rawSize = raw.size,
            declaredInnerLength = declared,
            headerSize = 4,
            payload = raw.copyOfRange(4, raw.size),
            flags = flags
        )
    }

    /** Small deterministic summary for the protocol log; never emits guessed signal names. */
    fun summary(raw: ByteArray): String {
        val unwrapped = unwrap(raw) ?: return "wrapper=NOT_CONFIRMED rawBytes=${raw.size}"
        val headWords = wordPreview(unwrapped.payload, 0, 8)
        val tailWords = wordPreview(unwrapped.payload, (unwrapped.payload.size / 4 - 4).coerceAtLeast(0), 4)
        return "wrapper=CONFIRMED rawBytes=${raw.size} declaredInnerLen=${unwrapped.declaredInnerLength} " +
            "payloadBytes=${unwrapped.payload.size} " +
            "flags=0x%04X headU32LE=[%s] tailU32LE=[%s]".format(
                unwrapped.flags,
                headWords,
                tailWords
            )
    }

    private fun wordPreview(bytes: ByteArray, startWord: Int, count: Int): String {
        if (bytes.isEmpty()) return ""
        val first = startWord.coerceAtLeast(0) * 4
        if (first >= bytes.size) return ""
        val end = minOf(bytes.size, first + count.coerceAtLeast(0) * 4)
        val words = ArrayList<String>()
        var o = first
        while (o + 4 <= end) {
            words += "%d:%08X".format(o, le32(bytes, o).toLong() and 0xFFFF_FFFFL)
            o += 4
        }
        return words.joinToString(",")
    }

    private fun le16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun le32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}
