package com.jarvis.assistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R

/**
 * A foreground service with type "microphone", as required by Android 14+ for any service
 * that uses the mic while not directly in a foreground Activity.
 *
 * IMPORTANT / HONEST LIMITATION: this keeps a listening session alive while the app is
 * running (e.g. screen on, notification shown), and is what lets Jarvis show a persistent
 * "Jarvis is listening" notification. It CANNOT record audio while the device is actually
 * locked with the screen off — Android does not allow third-party apps to do that, for
 * privacy/security reasons. Only apps set as the device's default Assistant get closer
 * integration (see MainActivity's ACTION_ASSIST intent filter), and even then, full
 * always-on background listening ("Hey Jarvis" while screen is off) is not something a
 * regular app can implement outside of an approved wake-word/Assistant framework.
 */
class JarvisForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_ID)
            .setContentTitle("Jarvis is active")
            .setContentText("Tap to open, or say your command.")
            .setSmallIcon(R.drawable.ic_jarvis_notification)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
