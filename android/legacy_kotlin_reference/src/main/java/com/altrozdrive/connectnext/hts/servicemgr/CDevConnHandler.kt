package com.altrozdrive.connectnext.hts.servicemgr

class CDevConnHandler(private val serviceMgr: CServiceMgr) {
    fun serviceAdvertised(name: String) = serviceMgr.onServiceAvailable(name)
    fun serviceConnected(name: String, connectionId: Int): Boolean = serviceMgr.connect(name, connectionId) != null
    fun serviceDisconnected(name: String) = serviceMgr.disconnect(name)
}
