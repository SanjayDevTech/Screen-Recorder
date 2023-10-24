package com.sanjay.ezyscreenrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.util.TypedValue
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


object Utils {
    private const val CHANNEL_ID = "Recording_Service"
    fun Service.buildNotification(pendingIntent: PendingIntent? = null): Notification {

        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Channel title",
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Service")
            .setContentText("Recording")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .apply {
                addAction(R.drawable.baseline_stop_24, "Stop", pendingIntent)
            }
            .build()
    }


    fun Context.hasPermissions(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ComponentActivity.getScreenDensity(): Int {
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

    fun ComponentActivity.getRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: windowManager.defaultDisplay.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
    }

    fun ComponentActivity.getSize(): Size {
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

    fun getCompatibleScreenSize(width: Int, height: Int): Size {
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