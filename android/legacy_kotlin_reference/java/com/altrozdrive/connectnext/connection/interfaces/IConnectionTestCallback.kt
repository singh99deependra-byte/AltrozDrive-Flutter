package com.altrozdrive.connectnext.connection.interfaces

interface IConnectionTestCallback {
    fun onResult(success: Boolean, detail: String? = null)
}
