package com.sanjay.ezyscreenrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class RecordingEventReceiver : BroadcastReceiver() {

    companion object {
        const val EVENT_START_RECORDING = "Event:Start_Recording"
        const val EVENT_STOP_RECORDING = "Event:Stop_Recording"
    }
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            EVENT_START_RECORDING -> {
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("${context.packageName}.RECORDING_EVENT").also {
                    it.putExtra("action", "START")
                })
            }
            EVENT_STOP_RECORDING -> {
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("${context.packageName}.RECORDING_EVENT").also {
                    it.putExtra("action", "STOP")
                })
            }
        }
    }
}
