package com.altrozdrive.app

import android.service.notification.NotificationListenerService

/** Android system adapter required for the media-access permission screen.
 * Dart media-session state/control bridging still needs to be ported and tested. */
class MediaNotificationListener : NotificationListenerService()
