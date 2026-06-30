package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.model.MediaClip
import com.example.data.model.Subtitle
import com.example.data.model.Voiceover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class VideoExportService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val ffmpegEngine = FFmpegEngine()

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "VideoExportChannel"
        private const val TAG = "VideoExportService"

        val isExporting = MutableStateFlow(false)
        val exportProgress = MutableStateFlow(0f)
        val exportMessage = MutableStateFlow<String?>(null)
        val exportFilePath = MutableStateFlow<String?>(null)

        fun startExport(context: Context, projectId: Int) {
            val intent = Intent(context, VideoExportService::class.java).apply {
                putExtra("PROJECT_ID", projectId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectId = intent?.getIntExtra("PROJECT_ID", -1) ?: -1
        if (projectId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceWithNotification()

        isExporting.value = true
        exportProgress.value = 0f
        exportMessage.value = "Initializing news video compilation..."

        scope.launch {
            try {
                // Initialize Room DB
                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "news_studio_editor.db"
                ).fallbackToDestructiveMigration().build()

                val project = database.projectDao().getProjectById(projectId)
                if (project == null) {
                    exportMessage.value = "Error: Project not found"
                    isExporting.value = false
                    updateNotification("Export failed: Project not found", 0, true)
                    stopSelf()
                    return@launch
                }

                val clips = try {
                    Json.decodeFromString<List<MediaClip>>(project.clipsJson)
                } catch (e: Exception) {
                    emptyList()
                }

                val voiceovers = try {
                    Json.decodeFromString<List<Voiceover>>(project.voiceoversJson)
                } catch (e: Exception) {
                    emptyList()
                }

                val subtitles = try {
                    Json.decodeFromString<List<Subtitle>>(project.subtitlesJson)
                } catch (e: Exception) {
                    emptyList()
                }

                if (clips.isEmpty()) {
                    exportMessage.value = "Timeline is empty. Please add video clips."
                    isExporting.value = false
                    updateNotification("Export failed: Timeline is empty", 0, true)
                    stopSelf()
                    return@launch
                }

                val cacheDir = cacheDir
                val galleryDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "AI_News_Studio_Editor"
                )
                if (!galleryDir.exists()) {
                    galleryDir.mkdirs()
                }

                val outputFile = File(galleryDir, "News_Studio_Export_${System.currentTimeMillis()}.mp4")
                exportMessage.value = "Rendering news banners and stitching clips in high-speed mode..."
                updateNotification("Compiling news video... (0%)", 0, false)

                ffmpegEngine.compileProject(
                    project = project,
                    clips = clips,
                    voiceovers = voiceovers,
                    subtitles = subtitles,
                    cacheDir = cacheDir,
                    outputFile = outputFile,
                    onProgress = { progress ->
                        val combinedProgress = 0.3f + (progress * 0.6f)
                        exportProgress.value = combinedProgress
                        val percent = (combinedProgress * 100).toInt()
                        updateNotification("Compiling news video... ($percent%)", percent, false)
                    },
                    onComplete = { success, logs ->
                        isExporting.value = false
                        if (success) {
                            exportProgress.value = 1.0f
                            exportFilePath.value = outputFile.absolutePath
                            exportMessage.value = "Successfully compiled video!\nSaved to: ${outputFile.name}"
                            updateNotification("Successfully compiled: ${outputFile.name}", 100, true)
                        } else {
                            exportProgress.value = 0f
                            exportMessage.value = "Export failed. Please check files or codecs.\nLogs: ${logs?.take(120)}"
                            updateNotification("Export failed. Please check formats.", 0, true)
                        }
                        stopSelf()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error compiling in background service", e)
                exportMessage.value = "Export error: ${e.localizedMessage}"
                isExporting.value = false
                updateNotification("Export error occurred", 0, true)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("News Studio Export")
            .setContentText("Initializing background rendering...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(message: String, progress: Int, isFinished: Boolean) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("News Studio Export")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (!isFinished) {
            builder.setProgress(100, progress, false)
            builder.setOngoing(true)
        } else {
            builder.setProgress(0, 0, false)
            builder.setOngoing(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Video Export"
            val descriptionText = "Shows progress of video compiling in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
