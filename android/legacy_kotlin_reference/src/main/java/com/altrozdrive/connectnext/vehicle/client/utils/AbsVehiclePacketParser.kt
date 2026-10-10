package com.altrozdrive.connectnext.vehicle.client.utils

import com.altrozdrive.connectnext.vehicle.client.model.VO.VehiclePacketVO

abstract class AbsVehiclePacketParser(val family: String) {
    abstract fun canParse(raw: ByteArray): Boolean
    abstract fun parse(raw: ByteArray): VehiclePacketVO
    protected fun raw(raw: ByteArray, values: Map<String, Double> = emptyMap(), validated: Boolean = false) = VehiclePacketVO(family, raw.copyOf(), values, validated)
}
