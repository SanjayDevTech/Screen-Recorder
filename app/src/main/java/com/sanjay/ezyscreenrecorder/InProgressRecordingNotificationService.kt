package com.sanjay.ezyscreenrecorder

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import com.sanjay.ezyscreenrecorder.Utils.buildNotification

class InProgressRecordingNotificationService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForegroundNotification()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action ?: return START_NOT_STICKY
        when (action) {
            START_RECORDING -> {
                startForegroundNotification()
            }

            STOP_RECORDING -> {
                stopForegroundNotification()
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        const val START_RECORDING = "Action:Start_Recording"
        const val STOP_RECORDING = "Action:Stop_Recording"
    }

    private fun stopForegroundNotification() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification() {
        val stopBroadcastIntent = Intent(this, RecordingEventReceiver::class.java).also {
            it.action = RecordingEventReceiver.EVENT_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntentCompat.getBroadcast(
            this,
            0,
            stopBroadcastIntent,
            PendingIntent.FLAG_CANCEL_CURRENT,
            false
        )
        val notification = buildNotification(stopPendingIntent)
        ServiceCompat.startForeground(
            this,
            1,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            else 0
        )
    }
}
