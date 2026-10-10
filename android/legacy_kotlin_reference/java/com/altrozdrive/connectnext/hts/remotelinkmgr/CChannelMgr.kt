package com.altrozdrive.connectnext.hts.remotelinkmgr

import java.util.concurrent.ConcurrentHashMap

/** Clean-room channel registry corresponding to ConnectNext CChannelMgr. */
class CChannelMgr {
    data class CChannelEntry(val id: Int, val name: String, var open: Boolean = true)
    private val channels = ConcurrentHashMap<Int, CChannelEntry>()

    @Synchronized
    fun register(id: Int, name: String): CChannelEntry =
        CChannelEntry(id, name, true).also { channels[id] = it }

    fun open(id: Int) { channels[id]?.open = true }
    fun close(id: Int) { channels[id]?.open = false }
    fun remove(id: Int) { channels.remove(id) }
    fun get(id: Int): CChannelEntry? = channels[id]
    fun findByName(name: String): CChannelEntry? = channels.values.firstOrNull { it.name == name }
    fun all(): List<CChannelEntry> = channels.values.sortedBy { it.id }
    fun openChannels(): List<CChannelEntry> = channels.values.filter { it.open }.sortedBy { it.id }
    fun clear() = channels.clear()
}
