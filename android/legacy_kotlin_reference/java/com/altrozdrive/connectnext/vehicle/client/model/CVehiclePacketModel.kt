package com.altrozdrive.connectnext.vehicle.client.model
import com.altrozdrive.connectnext.vehicle.client.model.VO.VehiclePacketVO

class CVehiclePacketModel {
    private var latest: VehiclePacketVO? = null
    fun update(packet: VehiclePacketVO) { latest = packet }
    fun latest(): VehiclePacketVO? = latest
}
