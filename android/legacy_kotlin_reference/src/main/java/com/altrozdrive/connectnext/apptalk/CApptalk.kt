package com.altrozdrive.connectnext.apptalk

class CApptalk {
    data class CService(val name: String, val clientId: Int, val connectionId: Int)
    private val services = linkedMapOf<String, CService>()
    fun register(service: CService) { services[service.name] = service }
    fun unregister(name: String) { services.remove(name) }
    fun service(name: String): CService? = services[name]
    fun services(): List<CService> = services.values.toList()
}
