
package com.altrozdrive.app

import com.altrozdrive.connectnext.vehicle.client.utils.X451VehicleWrapper

/**
 * Live X451 VIS decoder.
 *
 * The transport/session is intentionally untouched. This class only consumes the
 * confirmed VehicleInfoService wrapper payload and looks for complete X451
 * callback-period records recovered from ConnectNext 6.40.
 *
 * Proven legacy signal ordering used here:
 * 100 ms:
 *   INSTRUMENT_CLUSTER_4:
 *     field 3 = fuel percentage (signal 57)
 *     field 4 = DTE (signal 43)
 *     field 5 = Trip A (signal 44, x0.1)
 *     field 6 = Trip B (signal 45, x0.1)
 *   INSTRUMENT_CLUSTER_5:
 *     field 2 = Avg Fuel Economy Trip A (signal 46, x0.1)
 *     field 3 = Avg Fuel Economy Trip B (signal 47, x0.1)
 *
 * 1000 ms:
 *   GLOBAL_SNAPSHOT_DATA_1 field 4 = ODO (signal 190)
 *
 * We require a complete structurally valid X451 period block before publishing
 * values. No hardcoded vehicle values are used.
 */
class X451LiveSignalDecoder {
    data class Values(
        val odo: Int? = null,
        val fuelPercent: Int? = null,
        val dteKm: Int? = null,
        val tripA: Float? = null,
        val tripB: Float? = null,
        val avgFuelEcoTripA: Float? = null,
        val avgFuelEcoTripB: Float? = null,
        val rpm: Int? = null,
        val validated: Boolean = false,
        val source: String = "",
        /** True only when this frame itself carried a valid ODO field. */
        val odoFresh: Boolean = false,
        /** True only when this frame itself carried valid Trip A/B counters. */
        val tripCountersFresh: Boolean = false
    )

    /*
     * Evidence-backed offsets are relative to the confirmed 4-byte X451 wrapper
     * payload (i.e. after [LE16 length][LE16 flags]). The same offsets occur in
     * the 2026-09-19 captures and reproduce the previously observed live values.
     *
     * 614-byte HTS body -> 610-byte payload:
     *   +402 LE32 fuel percentage
     *   +406 LE32 DTE km
     *   +410 LE32 Trip A raw / 10
     *   +414 LE32 Trip B raw / 10
     *   +434 LE32 Avg Economy A raw / 10
     *   +438 LE32 Avg Economy B raw / 10
     *
     * 234-byte HTS body -> 230-byte payload:
     *   +38 LE32 ODO km
     *
     * These offsets are deliberately fixed to the captured X451 layout; no
     * arbitrary scanning or hardcoded vehicle values are used.
     */
    private companion object {
        const val MIN_614_PAYLOAD = 442
        const val FUEL_OFFSET = 402
        const val DTE_OFFSET = 406
        const val TRIP_A_OFFSET = 410
        const val TRIP_B_OFFSET = 414
        const val AVG_A_OFFSET = 434
        const val AVG_B_OFFSET = 438
        const val RPM_OFFSET = 194
        const val ODO_1000_OFFSET = 38
    }

    private var last = Values()
    private var rpmEvidenceFrames = 0

    fun reset() {
        last = Values()
        rpmEvidenceFrames = 0
    }

    fun consume(raw: ByteArray): Values? {
        val unwrapped = X451VehicleWrapper.unwrap(raw) ?: return null
        val p = unwrapped.payload

        var odo: Int? = last.odo
        var fuel: Int? = last.fuelPercent
        var dte: Int? = last.dteKm
        var tripA: Float? = last.tripA
        var tripB: Float? = last.tripB
        var avgA: Float? = last.avgFuelEcoTripA
        var avgB: Float? = last.avgFuelEcoTripB
        var rpm: Int? = last.rpm
        var source = ""
        var odoFresh = false
        var tripCountersFresh = false

        // The 614-byte VehicleInfo body is the live cluster/fuel packet.
        if (raw.size == 614 && p.size >= MIN_614_PAYLOAD) {
            val f = le32(p, FUEL_OFFSET)
            val r = le32(p, DTE_OFFSET)
            val a = le32(p, TRIP_A_OFFSET)
            val b = le32(p, TRIP_B_OFFSET)
            val aa = le32(p, AVG_A_OFFSET)
            val ab = le32(p, AVG_B_OFFSET)
            val rpmCandidate = le16(p, RPM_OFFSET)

            val valuesValid =
                f in 0..100 &&
                r in 0..3000 &&
                a in 0..1_000_000 &&
                b in 0..1_000_000 &&
                aa in 0..10_000 &&
                ab in 0..10_000 &&
                rpmCandidate in 0..9_000

            if (valuesValid) {
                fuel = f
                dte = r
                tripA = a / 10f
                tripB = b / 10f
                tripCountersFresh = true
                avgA = aa / 10f
                avgB = ab / 10f
                rpmEvidenceFrames = (rpmEvidenceFrames + 1).coerceAtMost(3)
                if (rpmEvidenceFrames >= 3) rpm = rpmCandidate
                source = if (rpm != null) "X451/614-EVIDENCE-MAPPED+RPM/194-LE16" else "X451/614-EVIDENCE-MAPPED"
            }
        }

        // The 234-byte VehicleInfo body carries the 1000-ms snapshot including ODO.
        if (raw.size == 234 && p.size >= ODO_1000_OFFSET + 4) {
            val o = le32(p, ODO_1000_OFFSET)
            if (o in 1..500_000) {
                odo = o
                odoFresh = true
                source = if (source.isEmpty()) {
                    "X451/234-EVIDENCE-MAPPED"
                } else {
                    "$source+234"
                }
            }
        }

        // A partial/unrecognized frame must not turn into a live UI update.
        if (source.isEmpty()) return null

        last = Values(
            odo = odo,
            fuelPercent = fuel,
            dteKm = dte,
            tripA = tripA,
            tripB = tripB,
            avgFuelEcoTripA = avgA,
            avgFuelEcoTripB = avgB,
            rpm = rpm,
            validated = true,
            source = source,
            odoFresh = odoFresh,
            tripCountersFresh = tripCountersFresh
        )
        return last
    }

    private fun le16(b: ByteArray, offset: Int): Int =
        (b[offset].toInt() and 0xFF) or
            ((b[offset + 1].toInt() and 0xFF) shl 8)

    private fun le32(b: ByteArray, offset: Int): Int {
        return (b[offset].toInt() and 0xFF) or
            ((b[offset + 1].toInt() and 0xFF) shl 8) or
            ((b[offset + 2].toInt() and 0xFF) shl 16) or
            ((b[offset + 3].toInt() and 0xFF) shl 24)
    }
}
