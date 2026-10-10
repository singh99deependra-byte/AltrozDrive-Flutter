package com.altrozdrive.connectnext.apptalk.client.file
object FileFrameUtils { fun frame(payload: ByteArray): ByteArray { val out=java.io.ByteArrayOutputStream(payload.size+2); out.write(payload.size and 255); out.write(payload.size ushr 8 and 255); out.write(payload); return out.toByteArray() } }
