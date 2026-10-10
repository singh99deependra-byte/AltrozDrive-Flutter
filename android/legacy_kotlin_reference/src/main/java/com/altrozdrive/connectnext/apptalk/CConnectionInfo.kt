package com.altrozdrive.connectnext.apptalk

data class CConnectionInfo(val peerName: String?, val peerAddress: String?, val transport: String = "Bluetooth", val connected: Boolean = true)
