package com.sanjay.ezyscreenrecorder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.sanjay.ezyscreenrecorder.Utils.getRotation
import com.sanjay.ezyscreenrecorder.Utils.getScreenDensity
import com.sanjay.ezyscreenrecorder.Utils.getSize
import com.sanjay.ezyscreenrecorder.Utils.hasPermissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val COUNT_DOWN = 3
        val PARENT_DIRECTORY: String = Environment.DIRECTORY_DOWNLOADS
        const val DIRECTORY = "Ezy Recordings"
    }

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var countText: TextView

    private lateinit var screenRecorder: ScreenRecorder

    private val isNotificationPermissionGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    private val isRecordAudioPermissionGranted: Boolean
        get() = hasPermissions(Manifest.permission.RECORD_AUDIO)


    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (Manifest.permission.RECORD_AUDIO in permissions) {
                if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
                    startRecording()
                }
            }
            if (Manifest.permission.POST_NOTIFICATIONS in permissions) {
                if (permissions[Manifest.permission.POST_NOTIFICATIONS] == true) {
                    startButton.isEnabled = true
                    stopButton.isEnabled = false
                }
            }
        }

    private fun startForegroundService() {
        val serviceIntent = Intent(InProgressRecordingNotificationService.START_RECORDING).also {
            it.setClass(this, InProgressRecordingNotificationService::class.java)
        }
        ActivityCompat.startForegroundService(this, serviceIntent)
    }

    private fun startForegroundServiceReally() {
        val serviceIntent = Intent(InProgressRecordingNotificationService.START_RECORDING_REALLY).also {
            it.setClass(this, InProgressRecordingNotificationService::class.java)
        }
        ActivityCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopForegroundService() {
        val serviceIntent = Intent(InProgressRecordingNotificationService.STOP_RECORDING).also {
            it.setClass(this, InProgressRecordingNotificationService::class.java)
        }
        ActivityCompat.startForegroundService(this, serviceIntent)
    }

    private val requestScreenCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultCode = result.resultCode
            if (resultCode != RESULT_OK) {
                startButton.isEnabled = true
                return@registerForActivityResult
            }
            val data = result.data ?: return@registerForActivityResult

            startForegroundService()

            lifecycleScope.launch {
                repeat(COUNT_DOWN) {
                    countText.text = (COUNT_DOWN - it).toString()
                    delay(1000)
                }
                countText.text = ""
                mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
                val fileName = String.format(
                    "Recording_%s.mp4",
                    SimpleDateFormat("dd_MM_yyyy_hh_mm_ss_a", Locale.ENGLISH).format(
                        Calendar.getInstance().time
                    )
                )
                val folder =
                    File(
                        Environment.getExternalStoragePublicDirectory(PARENT_DIRECTORY),
                        DIRECTORY
                    )
                if (!folder.exists()) {
                    folder.mkdir()
                }
                val file = File(folder, fileName)
                val isStarted = screenRecorder.start(file, mediaProjection)
                startForegroundServiceReally()
                if (isStarted) {
                    stopButton.isEnabled = true
                } else {
                    startButton.isEnabled = true
                    stopForegroundService()
                }
            }
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("action") == "STOP") {
                stopRecording()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton = findViewById(R.id.start_record_button)
        stopButton = findViewById(R.id.stop_record_button)
        countText = findViewById(R.id.count_text)

        startButton.isEnabled = false
        stopButton.isEnabled = false
        val intentFilter = IntentFilter("$packageName.RECORDING_EVENT")
        val receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED
        ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter, receiverFlags)

        val screenDensity = getScreenDensity()
        val rotation = getRotation()
        val (width, height) = getSize()

        screenRecorder = ScreenRecorder(this, screenDensity, rotation, width, height)

        mediaProjectionManager =
            ContextCompat.getSystemService(this, MediaProjectionManager::class.java)

        startButton.setOnClickListener {
            if (!isRecordAudioPermissionGranted) {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                return@setOnClickListener
            }
            startRecording()
        }

        stopButton.setOnClickListener {
            stopRecording()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isNotificationPermissionGranted) {
                startButton.isEnabled = true
                stopButton.isEnabled = false
            } else {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        } else {
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }

    }

    private fun startRecording() {
        requestScreenCapture.launch(mediaProjectionManager?.createScreenCaptureIntent())
        startButton.isEnabled = false
    }

    private fun stopRecording() {
        stopForegroundService()
        val outputFileName = screenRecorder.stop()
        stopButton.isEnabled = false
        startButton.isEnabled = true
        Toast.makeText(
            this,
            "Saved to $PARENT_DIRECTORY > $DIRECTORY > $outputFileName",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        if (screenRecorder.isRecording) {
            stopRecording()
        }
    }

}