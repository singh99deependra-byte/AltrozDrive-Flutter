package com.altrozdrive.connectnext.apptalk
class CApptalkListener(val listener: IApptalkListener) { data class CTransaction(val id: Int, val request: ByteArray, var response: ByteArray? = null); fun onMessage(data: ByteArray) = listener.onMessage(data) }
class GenericClient(private val callback: IGenericClientCallback) { fun onData(data: ByteArray) = callback.onData(data) }
class GenericClientListenerImpl(private val callback: IGenericClientCallback) { fun onData(data: ByteArray) = callback.onData(data) }
class GenericClientManager { private val clients = mutableListOf<GenericClient>(); fun add(client: GenericClient) = clients.add(client); fun count(): Int = clients.size }
class SalServerConnection(val connection: CConnection)
class Log { fun d(@Suppress("UNUSED_PARAMETER") tag: String, @Suppress("UNUSED_PARAMETER") msg: String) = Unit }
