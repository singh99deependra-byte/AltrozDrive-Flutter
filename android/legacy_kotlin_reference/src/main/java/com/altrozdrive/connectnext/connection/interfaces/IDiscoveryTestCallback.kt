package com.altrozdrive.connectnext.connection.interfaces

interface IDiscoveryTestCallback {
    fun onDiscoveryResult(found: Boolean, detail: String? = null)
}
