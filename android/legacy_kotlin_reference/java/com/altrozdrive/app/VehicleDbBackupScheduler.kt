package com.altrozdrive.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** Schedules one local, inexact wall-clock backup each night and re-arms it after firing. */
object VehicleDbBackupScheduler {
    private const val ACTION = "com.altrozdrive.app.ACTION_NIGHTLY_DB_BACKUP"
    private const val REQUEST_CODE = 9101
    private const val WINDOW_MS = 30L * 60L * 1000L

    fun schedule(context: Context, hour: Int, minute: Int) {
        try {
            val app = context.applicationContext
            val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(app, VehicleDbBackupReceiver::class.java).setAction(ACTION)
            val pending = PendingIntent.getBroadcast(
                app,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            // Inexact scheduling is intentional: the backup is not user-visible timing
            // functionality and does not need exact-alarm special access.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                next.timeInMillis,
                WINDOW_MS,
                pending
            )
        } catch (_: Throwable) {
            // Backup scheduling must never crash the app.
        }
    }

    fun scheduleNextAfterRun(context: Context, hour: Int, minute: Int) =
        schedule(context.applicationContext, hour, minute)
}
