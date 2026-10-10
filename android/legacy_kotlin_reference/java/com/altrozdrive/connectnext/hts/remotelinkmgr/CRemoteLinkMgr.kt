package com.altrozdrive.connectnext.hts.remotelinkmgr

import com.altrozdrive.app.MirroredHtsFrame
import java.util.concurrent.ConcurrentHashMap

class CRemoteLinkMgr(
    val channelMgr: CChannelMgr = CChannelMgr(),
    val linkReader: CLinkReader
) {
    data class CLinkRegistryElem(val id: Int, val name: String, val connectionId: Int?)
    private val registry = ConcurrentHashMap<String, CLinkRegistryElem>()
    fun register(name: String, connectionId: Int?, @Suppress("UNUSED_PARAMETER") onFrame: (MirroredHtsFrame) -> Unit = {}): CLinkRegistryElem {
        val entry = CLinkRegistryElem(registry.size + 1, name, connectionId)
        registry[name] = entry
        if (connectionId != null) channelMgr.register(connectionId, name)
        return entry
    }
    fun find(name: String): CLinkRegistryElem? = registry[name]
    fun all(): List<CLinkRegistryElem> = registry.values.sortedBy { it.id }
    fun close() { registry.clear(); channelMgr.clear(); linkReader.reset() }
}
