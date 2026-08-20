package com.haji.racing.ui.record

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.haji.racing.R
import com.haji.racing.service.RecordingService

/**
 * 启动/停止前台记录服务
 */
object RecordingStarter {

    fun start(context: Context, trackUid: String?, mode: String) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_TRACK_UID, trackUid ?: "")
            putExtra(RecordingService.EXTRA_MODE, mode)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)
    }
}
