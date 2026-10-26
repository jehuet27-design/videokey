package com.example.videokey.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.videokey.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "videokey_screen_recording_channel"
        private const val NOTIF_ID = 9021

        const val ACTION_START = "com.example.videokey.action.START_SCREEN_REC"
        const val ACTION_STOP = "com.example.videokey.action.STOP_SCREEN_REC"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var lastRecordedFile: File? = null
            private set

        @Volatile
        var recordingStartTimeMs: Long = 0L
            private set

        fun getDurationSeconds(): Int {
            if (!isRunning || recordingStartTimeMs == 0L) return 0
            return ((System.currentTimeMillis() - recordingStartTimeMs) / 1000).toInt()
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }

                if (resultCode != 0 && data != null) {
                    startForegroundNotification()
                    startRecording(resultCode, data)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "VideoKey Screen Recording"
            val desc = "Recording screen with audio accompaniment and vocals"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 VideoKey: Kinukunan ang Screen")
            .setContentText("Kasalukuyang nagre-record ng screen kasama ang sound...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startRecording(resultCode: Int, data: Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "mediaProjection is null")
                stopSelf()
                return
            }

            // Register mandatory callback for Android 14+
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopRecording()
                    stopSelf()
                }
            }, null)

            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            var width = metrics.widthPixels
            var height = metrics.heightPixels
            val dpi = metrics.densityDpi

            // Round dimensions to even numbers for H.264 encoder
            width = (width / 2) * 2
            height = (height / 2) * 2

            // Target folder: /storage/emulated/0/VideoKey/recordings
            val primaryDir = File(Environment.getExternalStorageDirectory(), "VideoKey/recordings")
            val targetDir = if (primaryDir.exists() || primaryDir.mkdirs()) {
                primaryDir
            } else {
                File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "recordings").apply { mkdirs() }
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            currentOutputFile = File(targetDir, "VideoKey_Screen_$timestamp.mp4")

            var mrSuccess = false
            val candidateSources = intArrayOf(
                MediaRecorder.AudioSource.CAMCORDER,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.DEFAULT
            )
            for (source in candidateSources) {
                try {
                    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(this)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    mr.setAudioSource(source)
                    mr.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    mr.setOutputFile(currentOutputFile!!.absolutePath)
                    mr.setVideoSize(width, height)
                    mr.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    mr.setVideoEncodingBitRate(6_000_000)
                    mr.setVideoFrameRate(30)
                    mr.setAudioSamplingRate(44100)
                    mr.setAudioEncodingBitRate(256000)
                    mr.prepare()
                    mediaRecorder = mr
                    mrSuccess = true
                    Log.i(TAG, "MediaRecorder initialized with audio source $source")
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Audio source $source failed: ${e.message}, trying next...")
                }
            }
            if (!mrSuccess) {
                throw RuntimeException("Could not initialize MediaRecorder with any audio source")
            }

            val surface = mediaRecorder!!.surface
            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "VideoKeyScreenDisplay",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )

            mediaRecorder!!.start()
            recordingStartTimeMs = System.currentTimeMillis()
            isRunning = true
            lastRecordedFile = currentOutputFile
            Log.i(TAG, "Screen recording with sound started: ${currentOutputFile!!.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording with sound", e)
            stopRecording()
            stopSelf()
        }
    }

    private fun stopRecording() {
        if (!isRunning) return
        isRunning = false
        recordingStartTimeMs = 0L

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder stop warning", e)
        }
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder release warning", e)
        }
        mediaRecorder = null

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "VirtualDisplay release warning", e)
        }
        virtualDisplay = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "MediaProjection stop warning", e)
        }
        mediaProjection = null

        currentOutputFile?.let { file ->
            lastRecordedFile = file
            // Trigger MediaScanner so video is visible immediately
            try {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = android.net.Uri.fromFile(file)
                }
                sendBroadcast(mediaScanIntent)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
