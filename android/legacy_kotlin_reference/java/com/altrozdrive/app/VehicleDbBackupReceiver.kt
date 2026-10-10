package com.altrozdrive.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class VehicleDbBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                if (intent?.action == "com.altrozdrive.app.ACTION_NIGHTLY_DB_BACKUP") {
                    try { val db = VehicleSnapshotDb(app); try { db.purgeHistoryOlderThanDays(180L) } finally { db.close() } } catch (_: Throwable) { }
                    VehicleDbBackup.backupNow(app)
                    VehicleDbBackupScheduler.scheduleNextAfterRun(app, 1, 30)
                } else {
                    VehicleDbBackup.requestBackup(app)
                }
            } catch (_: Throwable) {
                // Never allow an alarm receiver failure to take down the application process.
            } finally {
                pending.finish()
            }
        }.start()
    }
}
