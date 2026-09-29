package com.example.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.moodle.UcfMoodleClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background sync service for Moodle (National network in Cuba / Intranet).
 * Continuously checks the Moodle server directly for incoming messages/evidences
 * without relying on Google Play Services or Firebase.
 */
class MoodleBackgroundSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null
    private val moodleClient = UcfMoodleClient()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MoodleBackgroundSyncService created")
        NotificationHelper.createNotificationChannel(applicationContext)
        startSyncLoop()
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val userDao = db.userDao()
            val messageDao = db.messageDao()
            val moodleConfigDao = db.moodleConfigDao()

            while (isActive) {
                try {
                    val config = moodleConfigDao.getConfig()
                    val currentUser = userDao.getCurrentUser()

                    if (config != null && config.host.isNotBlank() && currentUser != null) {
                        moodleClient.configure(config.host, config.repoId)
                        if (config.username.isNotBlank() && config.password.isNotBlank()) {
                            moodleClient.login(config.username, config.password)
                        }

                        // Check remote evidences on Moodle for incoming messages
                        val evidencesResult = moodleClient.getEvidences()
                        if (evidencesResult.isSuccess) {
                            val evidences = evidencesResult.getOrNull() ?: emptyList()
                            val cleanCurrent = currentUser.username.trim().removePrefix("@").lowercase()

                            for (evidence in evidences) {
                                if (evidence.name.startsWith("[ChatPro_Msg]") || evidence.name.startsWith("[ChatPro_DM]")) {
                                    val isDirectForMe = evidence.name.contains("_to_${cleanCurrent}")
                                    if (isDirectForMe) {
                                        val existing = messageDao.getMessageById(evidence.id)
                                        if (existing == null) {
                                            val senderPart = evidence.name.substringAfter("[ChatPro_DM] ").substringBefore("_to_").trim()
                                            val senderDisplay = senderPart.ifBlank { "Usuario" }
                                            NotificationHelper.showMessageNotification(
                                                context = applicationContext,
                                                chatId = "dm_${senderPart}_$cleanCurrent",
                                                senderName = senderDisplay,
                                                messageText = "Nuevo mensaje en la red nacional",
                                                isGroup = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Sync cycle notice: ${e.message}")
                }

                // Poll every 25 seconds
                delay(25_000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        Log.d(TAG, "MoodleBackgroundSyncService destroyed")
    }

    companion object {
        private const val TAG = "MoodleSyncService"

        fun start(context: Context) {
            val intent = Intent(context, MoodleBackgroundSyncService::class.java)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not start sync service: ${e.message}")
            }
        }
    }
}
