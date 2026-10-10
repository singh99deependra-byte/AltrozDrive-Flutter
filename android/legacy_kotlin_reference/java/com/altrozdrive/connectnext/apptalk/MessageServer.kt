package com.altrozdrive.connectnext.apptalk

class MessageServer {
    enum class eMsgType { CONNECT, DISCONNECT, DATA, ERROR }
    data class SubscribeElement(val name: String, val callback: (ByteArray) -> Unit)
    private val subscribers = mutableListOf<SubscribeElement>()
    fun subscribe(name: String, callback: (ByteArray) -> Unit) { synchronized(subscribers) { subscribers += SubscribeElement(name, callback) } }
    fun publish(data: ByteArray) { synchronized(subscribers) { subscribers.toList() }.forEach { it.callback(data.copyOf()) } }
}
