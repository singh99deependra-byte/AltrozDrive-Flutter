package com.altrozdrive.connectnext.apptalk.client.file
class ByteArrayBuffer(private val capacity: Int = 8192) { private val out=java.io.ByteArrayOutputStream(capacity); fun append(bytes: ByteArray){out.write(bytes)}; fun bytes():ByteArray=out.toByteArray(); fun clear()=out.reset() }
