package com.altrozdrive.connectnext.hts

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Small scheduler abstraction for heartbeat/monitor tasks. */
class RepeatingTask(private val executor: ScheduledExecutorService) {
    fun start(initialDelayMs: Long, periodMs: Long, task: () -> Unit) =
        executor.scheduleAtFixedRate(task, initialDelayMs, periodMs, TimeUnit.MILLISECONDS)
}
