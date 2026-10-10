package com.altrozdrive.connectnext.connection

import java.io.Closeable
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Serialized writer corresponding to ConnectNext's WriteThreadMessageProcessor role.
 * It guarantees ordered writes and keeps transport I/O out of the session state machine.
 */
class WriteThreadMessageProcessor(private val output: OutputStream) : Closeable {
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val running = AtomicBoolean(true)
    private val worker = Thread({ runLoop() }, "ConnectNext-WriteThreadMessageProcessor")

    init { worker.start() }

    fun enqueue(message: ByteArray) {
        require(running.get()) { "Writer is closed" }
        queue.put(message.copyOf())
    }

    private fun runLoop() {
        try {
            while (running.get()) {
                val msg = queue.poll(250, TimeUnit.MILLISECONDS) ?: continue
                output.write(msg)
                output.flush()
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            try { output.flush() } catch (_: Throwable) {}
        }
    }

    fun pendingCount(): Int = queue.size

    override fun close() {
        if (!running.compareAndSet(true, false)) return
        worker.interrupt()
        queue.clear()
    }
}
