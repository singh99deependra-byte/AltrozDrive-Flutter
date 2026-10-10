package com.altrozdrive.connectnext.session

import com.altrozdrive.app.ConnectNextHtsWire
import com.altrozdrive.app.ConnectNextMessageClassifier
import com.altrozdrive.app.ConnectNextMessageKind
import com.altrozdrive.app.MirroredHtsFrame
import com.altrozdrive.connectnext.hts.servicemgr.CServiceMgr

class ConnectNextSessionStateMachine(private val serviceMgr: CServiceMgr = CServiceMgr()) {
    private val vehicleServices = setOf("W3WService", "VehicleInfoService", "DriveNextService", "DoodleService", "RCAService")
    var state: ConnectNextSessionState = ConnectNextSessionState()
        private set

    fun reset() { serviceMgr.clear(); state = ConnectNextSessionState() }

    fun accept(frame: MirroredHtsFrame) {
        if (!frame.checksumValid && frame.messageType != null) {
            state = state.copy(invalidChecksumFrames = state.invalidChecksumFrames + 1, lastSequence = frame.sequence)
            return
        }
        val kind = ConnectNextMessageClassifier.classify(frame)
        state = state.copy(validFrames = state.validFrames + 1, lastSequence = frame.sequence)
        if (kind == ConnectNextMessageKind.ACK) return
        if (frame.messageType == ConnectNextHtsWire.NETWORK_ID) {
            val text = bytesToText(frame.body)
            parseNetwork(text)
            return
        }
        if (frame.messageType == ConnectNextHtsWire.VEHICLE_SERVICE) {
            if (state.stage.ordinal < SessionStage.VEHICLE_INFO_ACTIVE.ordinal) state = state.copy(stage = SessionStage.VEHICLE_INFO_ACTIVE)
            return
        }
        if (frame.messageType == 0x13 || frame.messageType == 0x14) processText(bytesToText(frame.body))
        else state = state.copy(unknownFrames = state.unknownFrames + 1)
    }

    private fun parseNetwork(text: String) {
        val t = text.trim('\u0000', ' ', '\n', '\r')
        val version = Regex("""\"HTS_VERSION\"\s*:\s*\"([^\"]+)\"""").find(t)?.groupValues?.getOrNull(1)
        val peer = Regex("""\"PEER_DEVICE_ID\"\s*:\s*(-?\d+)""").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()
        state = state.copy(stage = SessionStage.NETWORK_EXCHANGED, htsVersion = version ?: state.htsVersion, peerDeviceId = peer ?: state.peerDeviceId)
    }

    private fun processText(text: String) {
        val t = text.trim('\u0000', ' ', '\n', '\r')
        when {
            t.contains("HTS_VERSION", true) -> state = state.copy(stage = SessionStage.HTS_SEEN, htsVersion = Regex("HTS_VERSION[^0-9]*([0-9.]+)").find(t)?.groupValues?.getOrNull(1) ?: state.htsVersion)
            t.startsWith("Sl_Inf_Dev_Details") -> state = state.copy(stage = SessionStage.DEVICE_DETAILS)
            t.startsWith("Sl_Supported_Apps") || t.startsWith("Sl_Supported_App_Ack") -> state = state.copy(stage = SessionStage.APPS_NEGOTIATED)
            t.startsWith("Srvc_Available,") -> Regex("Srvc_Available,([^,]+)").findAll(t).forEach { serviceMgr.onServiceAvailable(it.groupValues[1]); state = state.copy(stage = SessionStage.SERVICES_NEGOTIATING, servicesAvailable = serviceMgr.availableServices().filter { it in vehicleServices }) }
            t.startsWith("Resp_Remote_Client_Con,") -> Regex("Resp_Remote_Client_Con,([^,]+),Srvc_Conn_Success,(-?\\d+)").find(t)?.let { m ->
                val service = m.groupValues[1]; val id = m.groupValues[2].toIntOrNull() ?: -1; serviceMgr.onServiceAvailable(service); serviceMgr.connect(service, id)
                val conn = state.servicesConnected + service
                state = state.copy(stage = if (service == "VehicleInfoService") SessionStage.VEHICLE_INFO_ACTIVE else SessionStage.SERVICES_NEGOTIATING, servicesConnected = conn.distinct(), vehicleInfoConnectionId = if (service == "VehicleInfoService") id else state.vehicleInfoConnectionId)
            }
        }
    }

    private fun bytesToText(body: ByteArray): String {
        var start = 0
        if (body.size >= 2) {
            val len = (body[0].toInt() and 255) or ((body[1].toInt() and 255) shl 8)
            if (len in 2..body.size) start = 2
        }
        return body.copyOfRange(start, body.size).toString(Charsets.UTF_8)
    }
}
