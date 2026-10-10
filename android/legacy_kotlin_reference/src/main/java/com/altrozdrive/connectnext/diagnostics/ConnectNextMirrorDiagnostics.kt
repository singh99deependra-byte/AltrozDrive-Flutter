package com.altrozdrive.connectnext.diagnostics

import com.altrozdrive.connectnext.session.ConnectNextSessionState

data class ConnectNextMirrorDiagnostics(
    val transport: String = "Bluetooth RFCOMM SppLink",
    val serviceUuid: String = "00000000-0000-0000-0000-000000000004",
    val clientId: Int = 20,
    val session: ConnectNextSessionState,
    val parserMode: String = "VALIDATED_ONLY"
)
