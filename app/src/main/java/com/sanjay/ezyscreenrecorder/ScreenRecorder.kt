package com.sanjay.ezyscreenrecorder

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import java.io.File
import java.io.IOException

class ScreenRecorder(
    context: Context
) {
    private lateinit var mScreenSize: Size
    private lateinit var mProfile: CamcorderProfile
    private var mRotation: Int = 0
    private var mScreenDensity: Int = 0

    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private val mMediaRecorder: MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    private lateinit var mFile: File
    private val mMediaProjectionCallback: MediaProjection.Callback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                mMediaRecorder.stop()
                mMediaRecorder.reset()
                mVirtualDisplay?.release()
            }
        }

    @Throws(IOException::class)
    private fun initRecorder(file: File) {
        mFile = file
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        val orientation = ORIENTATIONS[mRotation + 90]
        mMediaRecorder.setOrientationHint(orientation)
        mMediaRecorder.setProfile(mProfile)
        mMediaRecorder.setOutputFile(file.absolutePath)
        mMediaRecorder.prepare()
    }

    fun prepare(screenDensity: Int, rotation: Int, screenSize: Size) {
        val compatibleScreenSize = Utils.compatibleScreenSize(screenSize.width, screenSize.height)
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).also {
            it.videoFrameWidth = compatibleScreenSize.width
            it.videoFrameHeight = compatibleScreenSize.height
        }
        mScreenSize = compatibleScreenSize
        mProfile = profile
        mRotation = rotation
        mScreenDensity = screenDensity
    }

    fun start(file: File, mediaProjection: MediaProjection): Boolean {
        mMediaProjection = mediaProjection
        mediaProjection.registerCallback(mMediaProjectionCallback, null)
        try {
            initRecorder(file)
            mVirtualDisplay = mediaProjection.createVirtualDisplay(
                "Ezy Screen Recorder",
                mScreenSize.width,
                mScreenSize.height,
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
        return mFile
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
