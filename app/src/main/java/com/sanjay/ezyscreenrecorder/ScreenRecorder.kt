package com.sanjay.ezyscreenrecorder

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.SparseIntArray
import android.view.Surface
import java.io.File
import java.io.IOException

class ScreenRecorder(
    context: Context,
    private val mScreenDensity: Int,
    private val mRotation: Int,
    private val mWidth: Int,
    private val mHeight: Int
) {
    private val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).also {
        val size = Utils.getCompatibleScreenSize(mWidth, mHeight)
        it.videoFrameWidth = size.width
        it.videoFrameHeight = size.height
    }
    private val displayWidth = profile.videoFrameWidth
    private val displayHeight = profile.videoFrameHeight
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private val mMediaRecorder: MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    private var mFile: File? = null
    private val mMediaProjectionCallback: MediaProjection.Callback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                if (isRecording) {
                    mMediaRecorder.stop()
                    mMediaRecorder.reset()
                }
            }
        }

    @Throws(IOException::class)
    private fun initRecorder(file: File) {
        mFile = file
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        val orientation = ORIENTATIONS[mRotation + 90]
        mMediaRecorder.setOrientationHint(orientation)
        mMediaRecorder.setProfile(profile)

        mMediaRecorder.setOutputFile(file.absolutePath)
        mMediaRecorder.prepare()
    }

    fun start(file: File, mediaProjection: MediaProjection): Boolean {
        mMediaProjection = mediaProjection
        mediaProjection.registerCallback(mMediaProjectionCallback, null)
        try {
            initRecorder(file)
            mVirtualDisplay = mediaProjection.createVirtualDisplay(
                "Ezy Screen Recorder",
                displayWidth,
                displayHeight,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.surface,
                null,
                null
            )
            mMediaRecorder.start()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun stop(): File {
        if (!isRecording) {
            throw IllegalStateException("Screen Recorder is not in recording state. Cannot stop")
        }
        mMediaRecorder.stop()
        mMediaRecorder.reset()
        mVirtualDisplay?.release()
        mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
        mMediaProjection?.stop()
        mVirtualDisplay = null
        mMediaProjection = null
        return mFile ?: throw IllegalStateException("Output file is null. Cannot stop")
    }

    val isRecording: Boolean
        get() = mMediaProjection != null

    companion object {
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }
}
