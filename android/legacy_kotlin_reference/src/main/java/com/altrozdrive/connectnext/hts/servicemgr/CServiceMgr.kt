package com.altrozdrive.connectnext.hts.servicemgr

import com.altrozdrive.app.ConnectNextHtsWire
import java.util.concurrent.ConcurrentHashMap

/** Mirrors ConnectNext's service registry/connection manager role. */
class CServiceMgr {
    data class SrvcRegElem(val service: String, val available: Boolean = true, var connectionId: Int = -1)
    data class ConnRegElem(val service: String, val clientId: Int, val connectionId: Int)
    private val available = ConcurrentHashMap<String, SrvcRegElem>()
    private val connected = ConcurrentHashMap<String, ConnRegElem>()
    fun onServiceAvailable(name: String) { available[name] = SrvcRegElem(name) }
    fun isAvailable(name: String): Boolean = available[name]?.available == true
    fun connect(name: String, connectionId: Int): ConnRegElem? {
        if (!isAvailable(name)) return null
        return ConnRegElem(name, ConnectNextHtsWire.CLIENT_ID, connectionId).also { connected[name] = it; available[name]?.connectionId = connectionId }
    }
    fun disconnect(name: String) { connected.remove(name) }
    fun availableServices(): List<String> = available.keys.sorted()
    fun connectedServices(): List<ConnRegElem> = connected.values.sortedBy { it.service }
    fun clear() { available.clear(); connected.clear() }
}
