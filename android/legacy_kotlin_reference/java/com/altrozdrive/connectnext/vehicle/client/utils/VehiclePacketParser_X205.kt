package com.altrozdrive.connectnext.vehicle.client.utils

import com.altrozdrive.connectnext.vehicle.client.model.VO.VehiclePacketVO

class VehiclePacketParser_X205 : AbsVehiclePacketParser("X205") {
    override fun canParse(raw: ByteArray): Boolean = false
    override fun parse(raw: ByteArray): VehiclePacketVO = raw(raw)
}
