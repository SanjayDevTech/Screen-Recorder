package com.sanjay.ezyscreenrecorder

import android.app.Activity
import android.app.Application
import android.media.projection.MediaProjection
import androidx.lifecycle.AndroidViewModel
import com.sanjay.ezyscreenrecorder.Utils.screenDensity
import com.sanjay.ezyscreenrecorder.Utils.screenRotation
import com.sanjay.ezyscreenrecorder.Utils.windowSize
import java.io.File

class MainAppViewModel(application: Application) : AndroidViewModel(application) {
    private val screenRecorder = ScreenRecorder(application)

    fun startRecording(activity: Activity, file: File, mediaProjection: MediaProjection): Boolean {
        val screenDensity = activity.screenDensity()
        val rotation = activity.screenRotation()
        val screenSize = activity.windowSize()
        screenRecorder.prepare(screenDensity, rotation, screenSize)
        return screenRecorder.start(file, mediaProjection)
    }

    fun stopRecording(): File {
        return screenRecorder.stop()
    }

    fun isRecording() = screenRecorder.isRecording

    override fun onCleared() {
        super.onCleared()
        if (isRecording()) {
            stopRecording()
        }
    }

}
