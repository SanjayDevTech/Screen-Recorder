package com.sanjay.ezyscreenrecorder

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.util.TypedValue
import android.view.WindowInsets
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File


object Utils {
    private fun Context.openIntentForVideo(outFile: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW).also {
            val fileUri =
                FileProvider.getUriForFile(this, "$packageName.file_provider", outFile)
            it.setDataAndType(fileUri, "video/*")
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Open With")
    }

    fun Context.showVideoSavedNotification(file: File) {
        val pendingIntent = PendingIntentCompat.getActivity(
            this, 1, openIntentForVideo(file),
            PendingIntent.FLAG_ONE_SHOT, false
        )
        if (pendingIntent != null) {
            val notification =
                buildRecordingSavedNotification(
                    pendingIntent,
                    MainActivity.PARENT_DIRECTORY,
                    MainActivity.DIRECTORY
                )
            val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)
            nm?.notify(786, notification)
        }
        Toast.makeText(
            this,
            "Saved to ${MainActivity.PARENT_DIRECTORY} > ${MainActivity.DIRECTORY}",
            Toast.LENGTH_LONG
        ).show()
    }

    enum class NotificationChannelType(
        val channelId: String,
        val channelTitle: String,
        val importance: Int,
    ) {
        RecordingService(
            "Recording_Service",
            "Recording in-progress notification",
            NotificationManagerCompat.IMPORTANCE_MIN
        ),
        RecordingCompleted(
            "Recording_Completed",
            "Recording Preview notification",
            NotificationManagerCompat.IMPORTANCE_HIGH
        ),
    }


    fun Context.buildNotification(
        title: String,
        text: String,
        isOngoing: Boolean,
        channelType: NotificationChannelType,
        pendingIntent: PendingIntent? = null,
        action: String = "",
        actionPendingIntent: PendingIntent? = null,
    ): Notification {

        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm?.createNotificationChannel(
                NotificationChannel(
                    channelType.channelId,
                    channelType.channelTitle,
                    channelType.importance
                )
            )
        }

        return NotificationCompat.Builder(this, channelType.channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(isOngoing)
            .apply {
                if (actionPendingIntent != null) {
                    addAction(R.drawable.baseline_stop_24, action, actionPendingIntent)
                }
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                    setAutoCancel(true)
                }
            }
            .build()
    }

    fun Context.buildInProgressRecordingNotification(
        actionPendingIntent: PendingIntent? = null,
    ): Notification {
        return buildNotification(
            title = "Ezy Recording",
            text = "Recording is in progress",
            isOngoing = true,
            channelType = NotificationChannelType.RecordingService,
            pendingIntent = null,
            action = "Stop",
            actionPendingIntent = actionPendingIntent,
        )
    }

    fun Context.buildRecordingSavedNotification(
        pendingIntent: PendingIntent,
        parentDir: String,
        outDir: String,
    ): Notification {
        return buildNotification(
            title = "Recording saved",
            text = "Click to open the Recording",
            isOngoing = false,
            channelType = NotificationChannelType.RecordingCompleted,
            pendingIntent = pendingIntent,
        )
    }


    fun Context.hasPermissions(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun Activity.screenDensity(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val floatDensity = windowManager.currentWindowMetrics.density
            val floatDpi = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                floatDensity,
                resources.displayMetrics
            )
            floatDpi.toInt()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayMetrics = DisplayMetrics()
            (display ?: windowManager.defaultDisplay).getMetrics(displayMetrics)
            displayMetrics.densityDpi
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.densityDpi
        }
    }

    fun Activity.screenRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: windowManager.defaultDisplay.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
    }

    fun Activity.windowSize(): Size {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val windowInsets: WindowInsets = metrics.windowInsets
            val insets = windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars()
                        or WindowInsets.Type.displayCutout()
            )

            val insetsWidth: Int = insets.right + insets.left
            val insetsHeight: Int = insets.top + insets.bottom

            val bounds: Rect = metrics.bounds
            Size(
                bounds.width() - insetsWidth,
                bounds.height() - insetsHeight
            )
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay?.getMetrics(displayMetrics)
            Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }

    private val validWidthSizes = listOf(
        4320,
        2160,
        1440,
        1080,
        1088,
        720,
        480,
    ).sortedDescending()

    private val validHeightSizes = listOf(
        7680,
        4096,
        3840,
        2560,
        2048,
        1280,
        720,
        704,
        640,
    ).sortedDescending()

    fun compatibleScreenSize(width: Int, height: Int): Size {
        var outWidth = width
        var outHeight = height
        for (validWidth in validWidthSizes) {
            if (outWidth >= validWidth) {
                outWidth = validWidth
                break
            }
        }
        for (validHeight in validHeightSizes) {
            if (outHeight >= validHeight) {
                outHeight = validHeight
                break
            }
        }
        return Size(outWidth, outHeight)
    }
}