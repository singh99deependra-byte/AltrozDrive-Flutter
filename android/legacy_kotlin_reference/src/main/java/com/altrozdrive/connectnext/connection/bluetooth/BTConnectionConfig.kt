package com.altrozdrive.connectnext.connection.bluetooth

import java.util.UUID

data class BTConnectionConfig(
    val serviceName: String = "SppLink",
    val serviceUuid: UUID = UUID(0L, 4L),
    val maxFrameBytes: Int = 256 * 1024,
    val connectTimeoutMs: Long = 7000,
    val readPollMs: Long = 250
)
