package com.altrozdrive.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class VehicleDbBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        VehicleDbBackup.scheduleNightly(context.applicationContext)
    }
}
