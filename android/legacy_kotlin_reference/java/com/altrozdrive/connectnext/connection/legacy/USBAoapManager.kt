package com.altrozdrive.connectnext.connection.legacy

class USBAoapManager {
    var active: Boolean = false
        private set
    fun start() { active = true }
    fun stop() { active = false }
}
