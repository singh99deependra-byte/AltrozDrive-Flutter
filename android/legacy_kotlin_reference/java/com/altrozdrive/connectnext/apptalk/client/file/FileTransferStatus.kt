package com.altrozdrive.connectnext.apptalk.client.file
data class FileTransferStatus(val transferred: Long, val total: Long, val complete: Boolean, val error: String? = null)
