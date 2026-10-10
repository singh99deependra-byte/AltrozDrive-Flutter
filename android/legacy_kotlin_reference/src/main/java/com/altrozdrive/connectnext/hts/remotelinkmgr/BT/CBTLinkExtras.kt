package com.altrozdrive.connectnext.hts.remotelinkmgr.BT

class CBTLinkExtras {
    enum class eWriteState { IDLE, WRITING, COMPLETE, FAILED }
    class MultiFrameBufferUtility(private val maxBytes: Int = 256 * 1024) {
        private val buffer = java.io.ByteArrayOutputStream()
        @Synchronized fun add(bytes: ByteArray) { require(buffer.size() + bytes.size <= maxBytes); buffer.write(bytes) }
        @Synchronized fun snapshot(): ByteArray = buffer.toByteArray()
        @Synchronized fun clear() = buffer.reset()
    }
}
