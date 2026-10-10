package com.altrozdrive.connectnext.apptalk
fun interface IApptalkListener { fun onMessage(data: ByteArray) }
fun interface IRPCClientCallback { fun onResponse(data: ByteArray) }
fun interface IServiceConnection { fun onServiceState(service: String, connected: Boolean) }
fun interface IConnectionCallback { fun onConnection(info: CConnectionInfo) }
fun interface IGenericClientCallback { fun onData(data: ByteArray) }
fun interface IGenericServiceProxy { fun request(data: ByteArray): ByteArray }
