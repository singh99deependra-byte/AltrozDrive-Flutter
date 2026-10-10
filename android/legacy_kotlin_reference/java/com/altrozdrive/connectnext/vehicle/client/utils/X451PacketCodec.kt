package com.altrozdrive.connectnext.vehicle.client.utils

import java.util.concurrent.atomic.AtomicInteger

/**
 * Clean-room implementation of the byte-consumption rules recovered from
 * ConnectNext 6.40 VehiclePacketParser_X451 + AbsVehiclePacketParser.
 *
 * This class deliberately stops at a truncated record. It never invents an
 * offset, datatype, scale factor or engineering value.
 */
object X451PacketCodec {
    data class PacketSpec(val name: String, val formatCode: Int)

    private val fiveHundredMs = listOf(
        PacketSpec("BATTERY_MODULE_1", 3),
        PacketSpec("BATTERY_MODULE_2", 4),
        PacketSpec("BATTERY_MODULE_3", 3),
        PacketSpec("CLUSTER_TRAILER_MODULE_2", 26),
        PacketSpec("CLUSTER_TRAILER_MODULE_1", 4),
        PacketSpec("BODY_MODULE_1", 9),
        PacketSpec("BODY_MODULE_2", 12),
        PacketSpec("EMS2_FILTER_CL_LAMP", 1),
        PacketSpec("IPC7_DRV_MODE_FUN_VARIANT", 1),
        PacketSpec("EMS4_TXMN_VARIANT", 1),
        PacketSpec("PEPS2_RC", 1)
    )

    private val hundredMs = listOf(
        PacketSpec("BRAKE_MODULE_1", 2),
        PacketSpec("BRAKE_MODULE_2", 4),
        PacketSpec("CLIMATE_MODULE_INFO", 20),
        PacketSpec("CLIMATE_MODULE_1", 1),
        PacketSpec("ENGINE_MODULE_1", 2),
        PacketSpec("ENGINE_MODULE_2", 12),
        PacketSpec("ENGINE_MODULE_4", 2),
        PacketSpec("ENGINE_MODULE_5", 10),
        PacketSpec("ENGINE_MODULE_11", 2),
        PacketSpec("ENGINE_MODULE_12", 8),
        PacketSpec("ENGINE_MODULE_13", 1),
        PacketSpec("ENGINE_MODULE_14", 3),
        PacketSpec("STEERING_MODULE", 3),
        PacketSpec("INSTRUMENT_CLUSTER_1", 2),
        PacketSpec("INSTRUMENT_CLUSTER_2", 8),
        PacketSpec("INSTRUMENT_CLUSTER_4", 8),
        PacketSpec("INSTRUMENT_CLUSTER_5", 8),
        PacketSpec("INSTRUMENT_CLUSTER_6", 10),
        PacketSpec("AIRBAG_MODULE", 2),
        PacketSpec("TRANSMISSION_MODULE_1", 4),
        PacketSpec("TRANSMISSION_MODULE_3", 8),
        PacketSpec("IPC2_CUR_DRIVE_MODE_SIG", 1),
        PacketSpec("PEPS1_RC", 1),
        PacketSpec("SRS1_WARNING_LAMP", 1),
        PacketSpec("AMT_1", 1)
    )

    private val onChange = listOf(
        PacketSpec("AMBIENT_LIGHT_MODULE", 13),
        PacketSpec("CLIMATE_MODULE_CONFIG", 4),
        PacketSpec("CLIMATE_EVENT_INFO", 2),
        PacketSpec("MOODLIGHT_MODULE", 34),
        PacketSpec("HU_MOODLIGHT", 27)
    )

    private val thousandMs = listOf(
        PacketSpec("GLOBAL_SNAPSHOT_DATA_1", 4),
        PacketSpec("GLOBAL_SNAPSHOT_DATA_2", 7),
        PacketSpec("GLOBAL_SNAPSHOT_DATA_3", 1),
        PacketSpec("INSTRUMENT_CLUSTER_7", 1),
        PacketSpec("HU_INTERNAL_INFO", 9)
    )

    /**
     * Official parser accepts these periods as follows:
     * 500/1280 -> five-hundred-ms packet set
     * 100/256   -> hundred-ms packet set
     * 1000      -> thousand-ms packet set
     * 10/16     -> on-change packet set
     * 2000      -> no X451 packet set
     */
    fun decode(raw: ByteArray, periodMs: Int, cursorStart: Int = 0): X451DecodeResult {
        if (cursorStart < 0 || cursorStart > raw.size) {
            return X451DecodeResult(periodMs, emptyList(), cursorStart.coerceAtLeast(0).coerceAtMost(raw.size), false, "cursor out of bounds")
        }

        val specs = when (periodMs) {
            500, 1280 -> fiveHundredMs
            100, 256 -> hundredMs
            1000 -> thousandMs
            10, 16 -> onChange
            2000 -> emptyList()
            else -> return X451DecodeResult(periodMs, emptyList(), cursorStart, false, "unsupported X451 period=$periodMs")
        }
        if (specs.isEmpty()) return X451DecodeResult(periodMs, emptyList(), cursorStart, true)

        // The official generic packet parser is not a TLV parser. It consumes
        // a fixed number of bytes per packet based on `periodMs` and formatCode.
        // For on-change, the official base implementation treats the packet as
        // a String(byte[]) and then reads MsgAvlbl/EventPktChanged from the cursor;
        // live telemetry should therefore not be inferred from it.
        if (periodMs == 10 || periodMs == 16) {
            return decodeOnChangeEvidence(raw, periodMs, cursorStart, specs)
        }

        val cursor = AtomicInteger(cursorStart)
        val out = ArrayList<X451PacketRecord>(specs.size)
        for (spec in specs) {
            val start = cursor.get()
            val width = fieldWidth(periodMs)
            val needed = spec.formatCode * width + msgAvailableWidth(periodMs) + eventWidth(periodMs)
            if (cursor.get() + needed > raw.size) {
                return X451DecodeResult(
                    periodMs,
                    out,
                    cursor.get(),
                    false,
                    "truncated ${spec.name}: need=$needed remaining=${raw.size - cursor.get()}"
                )
            }
            val fields = ArrayList<Long>(spec.formatCode)
            repeat(spec.formatCode) {
                fields += when (periodMs) {
                    100 -> readLe64(raw, cursor)
                    256, 1000 -> readLeU16(raw, cursor).toLong()
                    500, 1280 -> readLe32(raw, cursor).toLong()
                    else -> error("unreachable")
                }
            }
            val msgAvailable = when (periodMs) {
                1000, 500 -> readLeU16(raw, cursor).toLong()
                256 -> readLe32(raw, cursor).toLong()
                100 -> readLeU16(raw, cursor).toLong()
                1280 -> readLe32(raw, cursor).toLong()
                else -> 0L
            }
            val event = when (periodMs) {
                16 -> readLe32(raw, cursor).toLong()
                else -> null
            }
            out += X451PacketRecord(
                packetName = spec.name,
                periodMs = periodMs,
                formatCode = spec.formatCode,
                fieldValues = fields,
                msgAvailable = msgAvailable,
                eventPktChanged = event,
                startOffset = start,
                endOffset = cursor.get()
            )
        }
        return X451DecodeResult(periodMs, out, cursor.get(), true)
    }

    /**
     * Evidence-preserving representation for the variable-length on-change path.
     * The official parser uses String(byte[]) for the packet payload, so there is
     * no safe fixed-width field map to expose here. We report only how much of the
     * trailing control area can be read exactly.
     */
    private fun decodeOnChangeEvidence(
        raw: ByteArray,
        periodMs: Int,
        cursorStart: Int,
        specs: List<PacketSpec>
    ): X451DecodeResult {
        if (cursorStart > raw.size) return X451DecodeResult(periodMs, emptyList(), cursorStart, false, "cursor out of bounds")
        val cursor = AtomicInteger(cursorStart)
        val out = ArrayList<X451PacketRecord>(specs.size)

        // No field bytes are consumed by String(byte[]) in the official base
        // method; the whole remaining byte[] is handed to String first. The
        // subsequent MsgAvlbl/EventPktChanged values are cursor-driven.
        val stringStart = cursor.get()
        for (index in specs.indices) {
            val isLast = index == specs.lastIndex
            if (!isLast) {
                out += X451PacketRecord(
                    packetName = specs[index].name,
                    periodMs = periodMs,
                    formatCode = specs[index].formatCode,
                    fieldValues = emptyList(),
                    msgAvailable = 0L,
                    eventPktChanged = null,
                    startOffset = stringStart,
                    endOffset = stringStart
                )
            } else {
                var event: Long? = null
                if (periodMs == 16) {
                    if (cursor.get() + 4 > raw.size) return X451DecodeResult(periodMs, out, cursor.get(), false, "truncated EventPktChanged")
                    event = readLe32(raw, cursor).toLong()
                }
                if (cursor.get() + 2 > raw.size) return X451DecodeResult(periodMs, out, cursor.get(), false, "truncated MsgAvlbl")
                val msgAvailable = readLeU16(raw, cursor).toLong()
                out += X451PacketRecord(
                    packetName = specs[index].name,
                    periodMs = periodMs,
                    formatCode = specs[index].formatCode,
                    fieldValues = emptyList(),
                    msgAvailable = msgAvailable,
                    eventPktChanged = event,
                    startOffset = stringStart,
                    endOffset = cursor.get()
                )
            }
        }
        return X451DecodeResult(periodMs, out, cursor.get(), true)
    }

    fun fieldWidth(periodMs: Int): Int = when (periodMs) {
        100 -> 8
        256, 1000 -> 2
        500, 1280 -> 4
        else -> 0
    }

    fun msgAvailableWidth(periodMs: Int): Int = when (periodMs) {
        100, 500, 1000 -> 2
        256, 1280 -> 4
        16 -> 2
        10 -> 2
        else -> 0
    }

    fun eventWidth(periodMs: Int): Int = when (periodMs) {
        16 -> 4
        10 -> 0
        else -> 0
    }

    private fun readLeU16(bytes: ByteArray, cursor: AtomicInteger): Int {
        val i = cursor.getAndIncrement()
        val j = cursor.getAndIncrement()
        return (bytes[i].toInt() and 0xFF) or ((bytes[j].toInt() and 0xFF) shl 8)
    }

    private fun readLe32(bytes: ByteArray, cursor: AtomicInteger): Int {
        val b0 = cursor.getAndIncrement()
        val b1 = cursor.getAndIncrement()
        val b2 = cursor.getAndIncrement()
        val b3 = cursor.getAndIncrement()
        return (bytes[b0].toInt() and 0xFF) or
            ((bytes[b1].toInt() and 0xFF) shl 8) or
            ((bytes[b2].toInt() and 0xFF) shl 16) or
            ((bytes[b3].toInt() and 0xFF) shl 24)
    }

    private fun readLe64(bytes: ByteArray, cursor: AtomicInteger): Long {
        var result = 0L
        for (shift in 0..7) {
            val i = cursor.getAndIncrement()
            result = result or ((bytes[i].toLong() and 0xFFL) shl (shift * 8))
        }
        return result
    }
}
