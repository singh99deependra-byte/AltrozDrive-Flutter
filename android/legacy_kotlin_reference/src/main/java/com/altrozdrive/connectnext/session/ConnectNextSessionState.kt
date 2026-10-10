package com.altrozdrive.connectnext.session

enum class SessionStage { DISCONNECTED, WAITING, HTS_SEEN, NETWORK_EXCHANGED, DEVICE_DETAILS, APPS_NEGOTIATED, SERVICES_NEGOTIATING, VEHICLE_INFO_ACTIVE }

data class ConnectNextSessionState(
    val stage: SessionStage = SessionStage.DISCONNECTED,
    val clientId: Int = 20,
    val htsVersion: String? = null,
    val peerDeviceId: Int? = null,
    val servicesAvailable: List<String> = emptyList(),
    val servicesConnected: List<String> = emptyList(),
    val vehicleInfoConnectionId: Int? = null,
    val lastSequence: Int? = null,
    val validFrames: Long = 0,
    val invalidChecksumFrames: Long = 0,
    val unknownFrames: Long = 0
)
