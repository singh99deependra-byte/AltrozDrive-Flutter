package com.altrozdrive.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Local, offline PUC/insurance document reminders. No cloud/service dependency. */
object VehicleReminderScheduler {
    const val PREFS = "altrozdrive_preferences"
    const val KEY_PUC_EXPIRY = "reminder_puc_expiry"
    const val KEY_INSURANCE_EXPIRY = "reminder_insurance_expiry"
    const val KEY_PUC_NUMBER = "reminder_puc_number"
    const val KEY_INSURANCE_NUMBER = "reminder_insurance_number"
    const val KEY_INSURER = "reminder_insurer"
    const val KEY_PUC_REMINDER_DAYS = "reminder_puc_days_before"
    const val KEY_INSURANCE_REMINDER_DAYS = "reminder_insurance_days_before"
    const val DEFAULT_REMINDER_DAYS = 30

    private const val CHANNEL_ID = "vehicle_reminders"
    private const val ACTION = "com.altrozdrive.app.VEHICLE_REMINDER"
    private const val EXTRA_TYPE = "type"
    private const val TYPE_PUC = "PUC"
    private const val TYPE_INSURANCE = "INSURANCE"
    private const val EXTRA_REMINDER_DATE = "reminder_date"
    private const val KEY_PUC_LAST_SENT = "reminder_puc_last_sent"
    private const val KEY_INSURANCE_LAST_SENT = "reminder_insurance_last_sent"
    private const val PUC_REQUEST_CODE = 8101
    private const val INSURANCE_REQUEST_CODE = 8102
    private const val REMINDER_HOUR = 9
    private const val REMINDER_MINUTE = 0

    fun save(
        context: Context,
        type: String,
        expiryMillis: Long,
        documentNumber: String = "",
        insurer: String = "",
        reminderDays: Int = DEFAULT_REMINDER_DAYS
    ) {
        val safeDays = reminderDays.coerceIn(1, 365)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (type == TYPE_PUC) {
                putLong(KEY_PUC_EXPIRY, expiryMillis)
                putString(KEY_PUC_NUMBER, documentNumber)
                putInt(KEY_PUC_REMINDER_DAYS, safeDays)
            } else if (type == TYPE_INSURANCE) {
                putLong(KEY_INSURANCE_EXPIRY, expiryMillis)
                putString(KEY_INSURANCE_NUMBER, documentNumber)
                putString(KEY_INSURER, insurer)
                putInt(KEY_INSURANCE_REMINDER_DAYS, safeDays)
            } else {
                return@apply
            }
        }.apply()
        cancel(context, type)
        scheduleNext(context, type)
    }

    fun clear(context: Context, type: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (type == TYPE_PUC) {
                remove(KEY_PUC_EXPIRY).remove(KEY_PUC_NUMBER).remove(KEY_PUC_LAST_SENT)
            } else if (type == TYPE_INSURANCE) {
                remove(KEY_INSURANCE_EXPIRY).remove(KEY_INSURANCE_NUMBER).remove(KEY_INSURER).remove(KEY_INSURANCE_LAST_SENT)
            }
        }.apply()
        cancel(context, type)
    }

    fun scheduleAll(context: Context) {
        scheduleNext(context, TYPE_PUC)
        scheduleNext(context, TYPE_INSURANCE)
    }

    private fun scheduleNext(context: Context, type: String) {
        cancel(context, type)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiry = expiryFor(prefs, type)
        if (expiry <= 0L) return

        val reminderDays = reminderDaysFor(prefs, type)
        val todayStart = startOfDay(System.currentTimeMillis())
        val expiryDay = startOfDay(expiry)
        val reminderStartDay = startOfDay(expiry - reminderDays * DAY_MS)

        // The daily cycle runs from the configured lead date through the expiry date.
        if (todayStart > expiryDay || todayStart < reminderStartDay) {
            if (todayStart < reminderStartDay) {
                scheduleAlarm(context, type, morningAt(reminderStartDay), reminderStartDay)
            }
            return
        }

        val lastSent = prefs.getString(lastSentKey(type), null)
        val todayKey = dateKey(todayStart)
        if (lastSent == todayKey) {
            val tomorrow = startOfDay(todayStart + DAY_MS)
            if (tomorrow <= expiryDay) scheduleAlarm(context, type, morningAt(tomorrow), tomorrow)
            return
        }

        // If the user saves/updates the reminder after 9 AM, send today's notification
        // shortly after saving rather than waiting until tomorrow. Otherwise use 9 AM.
        val morning = morningAt(todayStart)
        val now = System.currentTimeMillis()
        val triggerAt = if (now < morning) morning else now + 60_000L
        scheduleAlarm(context, type, triggerAt, todayStart)
    }

    private fun scheduleAlarm(context: Context, type: String, triggerAt: Long, reminderDate: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, VehicleReminderReceiver::class.java).apply {
            action = ACTION
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_REMINDER_DATE, reminderDate)
        }
        val requestCode = requestCodeFor(type)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun cancel(context: Context, type: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val requestCodes = listOf(requestCodeFor(type), legacyRequestCode(type, 100), legacyRequestCode(type, 101), legacyRequestCode(type, 102))
        requestCodes.forEach { requestCode ->
            val intent = Intent(context, VehicleReminderReceiver::class.java).apply { action = ACTION }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) alarm.cancel(pendingIntent)
        }
    }

    private fun expiryFor(prefs: android.content.SharedPreferences, type: String): Long = when (type) {
        TYPE_PUC -> prefs.getLong(KEY_PUC_EXPIRY, 0L)
        TYPE_INSURANCE -> prefs.getLong(KEY_INSURANCE_EXPIRY, 0L)
        else -> 0L
    }

    private fun reminderDaysFor(prefs: android.content.SharedPreferences, type: String): Int = when (type) {
        TYPE_PUC -> prefs.getInt(KEY_PUC_REMINDER_DAYS, DEFAULT_REMINDER_DAYS).coerceIn(1, 365)
        TYPE_INSURANCE -> prefs.getInt(KEY_INSURANCE_REMINDER_DAYS, DEFAULT_REMINDER_DAYS).coerceIn(1, 365)
        else -> DEFAULT_REMINDER_DAYS
    }

    private fun lastSentKey(type: String): String = when (type) {
        TYPE_PUC -> KEY_PUC_LAST_SENT
        TYPE_INSURANCE -> KEY_INSURANCE_LAST_SENT
        else -> ""
    }

    private fun requestCodeFor(type: String): Int = when (type) {
        TYPE_PUC -> PUC_REQUEST_CODE
        TYPE_INSURANCE -> INSURANCE_REQUEST_CODE
        else -> 8199
    }

    private fun legacyRequestCode(type: String, offset: Int): Int =
        (type.hashCode() and 0x7fffffff) % 100000 * 10 + offset

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun morningAt(dayStart: Long): Long = Calendar.getInstance().apply {
        timeInMillis = dayStart
        set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
        set(Calendar.MINUTE, REMINDER_MINUTE)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun dateKey(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Vehicle reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "PUC and insurance expiry reminders"
                }
            )
        }
    }

    fun notification(context: Context, type: String) {
        ensureChannel(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiry = expiryFor(prefs, type)
        if (expiry <= 0L) return

        val todayStart = startOfDay(System.currentTimeMillis())
        val expiryDay = startOfDay(expiry)
        val reminderStartDay = startOfDay(expiry - reminderDaysFor(prefs, type) * DAY_MS)
        if (todayStart < reminderStartDay || todayStart > expiryDay) {
            scheduleNext(context, type)
            return
        }

        val todayKey = dateKey(todayStart)
        if (prefs.getString(lastSentKey(type), null) == todayKey) {
            scheduleNext(context, type)
            return
        }

        val daysRemaining = ((expiryDay - todayStart) / DAY_MS).toInt().coerceAtLeast(0)
        val name = if (type == TYPE_PUC) "PUC certificate" else "Insurance"
        val title = if (daysRemaining == 0) "$name expires today" else "$name expiry reminder"
        val text = if (daysRemaining == 0) {
            "$name expires today. Open AltrozDrive to update it."
        } else {
            "$name expires in $daysRemaining days. Expiry: ${formatDate(expiry)}."
        }

        val openIntent = PendingIntent.getActivity(
            context,
            991 + type.hashCode().and(0x7fff),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(7000 + type.hashCode().and(0x7fff), notification)

        prefs.edit().putString(lastSentKey(type), todayKey).apply()
        scheduleNext(context, type)
    }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(millis))
}

class VehicleReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> VehicleReminderScheduler.scheduleAll(appContext)
            "com.altrozdrive.app.VEHICLE_REMINDER" -> VehicleReminderScheduler.notification(
                appContext,
                intent.getStringExtra("type") ?: return
            )
        }
    }
}
