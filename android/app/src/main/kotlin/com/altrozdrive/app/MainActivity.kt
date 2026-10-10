package com.altrozdrive.app

import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/** Flutter entry point for Option C. The channel below opens Android's media-access
 * settings page; vehicle logic is not yet ported or emulated here. */
class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.altrozdrive.app/system_settings")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "openNotificationListenerSettings" -> {
                        try {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            result.success(true)
                        } catch (e: Exception) {
                            result.error("SETTINGS_UNAVAILABLE", "Notification access settings could not be opened", null)
                        }
                    }
                    "isNotificationListenerEnabled" -> {
                        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
                        val allowed = enabled.split(":").any { flattened ->
                            try { ComponentName.unflattenFromString(flattened)?.packageName == packageName }
                            catch (_: Exception) { false }
                        }
                        result.success(allowed)
                    }
                    else -> result.notImplemented()
                }
            }
    }
}
