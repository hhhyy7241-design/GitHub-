package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TealDarkTextSecondary
import com.example.ui.theme.TealLightDivider
import com.example.ui.theme.TealLightTextSecondary
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.util.AudioCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    companion object {
        private const val TAG = "AudioRecorderHelper"
    }

    fun startRecording(): File? {
        stopAndRelease()

        return try {
            val audioDir = File(context.cacheDir, "audio_notes").apply {
                if (!exists()) mkdirs()
            }
            val file = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = rec
            isRecording = true
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording with AAC/MPEG4, trying fallback: ${e.message}")
            try {
                val audioDir = File(context.cacheDir, "audio_notes").apply {
                    if (!exists()) mkdirs()
                }
                val fallbackFile = File(audioDir, "audio_${System.currentTimeMillis()}.3gp")
                currentOutputFile = fallbackFile

                @Suppress("DEPRECATION")
                val fallbackRec = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(fallbackFile.absolutePath)
                    prepare()
                    start()
                }

                recorder = fallbackRec
                isRecording = true
                fallbackFile
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Audio recording fallback failed: ${fallbackEx.message}")
                stopAndRelease()
                null
            }
        }
    }

    fun stopRecording(): File? {
        val file = currentOutputFile
        if (!isRecording || recorder == null) {
            stopAndRelease()
            return file?.takeIf { it.exists() && it.length() > 50 }
        }

        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder stop failed: ${e.message}")
        } finally {
            stopAndRelease()
        }

        return if (file != null && file.exists() && file.length() > 50) {
            file
        } else {
            file?.delete()
            null
        }
    }

    fun cancelRecording() {
        stopAndRelease()
        currentOutputFile?.delete()
        currentOutputFile = null
    }

    private fun stopAndRelease() {
        isRecording = false
        try {
            recorder?.reset()
        } catch (ignored: Exception) {}
        try {
            recorder?.release()
        } catch (ignored: Exception) {}
        recorder = null
    }
}

/**
 * State machine for MediaPlayer playback
 */
enum class PlayerPlaybackState {
    IDLE,
    PREPARING,
    PLAYING,
    PAUSED,
    ERROR
}

@Composable
fun AudioMessageBubble(
    audioUrlOrPath: String,
    attachmentName: String? = null,
    isOutgoing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    var playbackState by remember { mutableStateOf(PlayerPlaybackState.IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }

    // Pre-calculate initial duration via background metadata retriever
    LaunchedEffect(audioUrlOrPath) {
        withContext(Dispatchers.IO) {
            val estimated = AudioCompressor.getAudioDurationMs(context, audioUrlOrPath)
            if (estimated > 0) {
                durationMs = estimated.toInt()
            }
        }
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun safelyReleasePlayer() {
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                player.setOnPreparedListener(null)
                player.setOnCompletionListener(null)
                player.setOnErrorListener(null)
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            } catch (e: Exception) {
                Log.w("AudioMessageBubble", "Safe release caught: ${e.message}")
            }
        }
    }

    DisposableEffect(audioUrlOrPath) {
        onDispose {
            safelyReleasePlayer()
        }
    }

    // Progress update timer
    LaunchedEffect(playbackState) {
        if (playbackState == PlayerPlaybackState.PLAYING) {
            while (playbackState == PlayerPlaybackState.PLAYING) {
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition
                            val dur = mp.duration
                            currentPositionMs = pos
                            if (dur > 0) {
                                durationMs = dur
                                progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore state errors during fast ticks
                }
                delay(100)
            }
        }
    }

    fun handlePlayPause() {
        when (playbackState) {
            PlayerPlaybackState.PLAYING -> {
                try {
                    mediaPlayer?.pause()
                    playbackState = PlayerPlaybackState.PAUSED
                } catch (e: Exception) {
                    safelyReleasePlayer()
                }
            }

            PlayerPlaybackState.PAUSED -> {
                try {
                    mediaPlayer?.start()
                    playbackState = PlayerPlaybackState.PLAYING
                } catch (e: Exception) {
                    safelyReleasePlayer()
                }
            }

            PlayerPlaybackState.PREPARING -> {
                // Ignore while preparing
            }

            PlayerPlaybackState.IDLE, PlayerPlaybackState.ERROR -> {
                safelyReleasePlayer()
                playbackState = PlayerPlaybackState.PREPARING

                try {
                    val player = MediaPlayer()
                    mediaPlayer = player

                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    when {
                        audioUrlOrPath.startsWith("http://") || audioUrlOrPath.startsWith("https://") -> {
                            player.setDataSource(audioUrlOrPath)
                        }
                        audioUrlOrPath.startsWith("content://") -> {
                            val uri = Uri.parse(audioUrlOrPath)
                            try {
                                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                                    player.setDataSource(pfd.fileDescriptor)
                                } ?: player.setDataSource(context, uri)
                            } catch (e: Exception) {
                                player.setDataSource(context, uri)
                            }
                        }
                        else -> {
                            val cleanPath = audioUrlOrPath.removePrefix("file://").removePrefix("file:")
                            val localFile = File(cleanPath)
                            if (localFile.exists() && localFile.canRead()) {
                                val fis = FileInputStream(localFile)
                                player.setDataSource(fis.fd)
                                fis.close()
                            } else {
                                player.setDataSource(context, Uri.parse(audioUrlOrPath))
                            }
                        }
                    }

                    player.setOnPreparedListener { mp ->
                        if (playbackState == PlayerPlaybackState.PREPARING) {
                            try {
                                durationMs = mp.duration
                                mp.start()
                                playbackState = PlayerPlaybackState.PLAYING
                            } catch (e: Exception) {
                                safelyReleasePlayer()
                            }
                        }
                    }

                    player.setOnCompletionListener {
                        playbackState = PlayerPlaybackState.PAUSED
                        progress = 0f
                        currentPositionMs = 0
                    }

                    player.setOnErrorListener { _, what, extra ->
                        Log.w("AudioMessageBubble", "MediaPlayer error: ($what, $extra)")
                        safelyReleasePlayer()
                        playbackState = PlayerPlaybackState.ERROR
                        true // Handled error safely
                    }

                    player.prepareAsync()
                } catch (e: Exception) {
                    Log.e("AudioMessageBubble", "Error initializing player: ${e.message}")
                    safelyReleasePlayer()
                    playbackState = PlayerPlaybackState.ERROR
                }
            }
        }
    }

    // Simulated waveform bar heights
    val waveHeights = remember {
        listOf(0.3f, 0.6f, 0.4f, 0.9f, 0.7f, 0.5f, 0.8f, 1.0f, 0.6f, 0.4f, 0.85f, 0.5f, 0.7f, 0.3f, 0.9f, 0.65f, 0.45f, 0.8f, 0.35f, 0.6f)
    }

    // Color definitions synchronized with Dark and Light themes
    val activeWaveColor: Color
    val inactiveWaveColor: Color
    val durationTextColor: Color
    val buttonBgColor: Color
    val buttonIconColor: Color

    if (isOutgoing) {
        if (isDark) {
            activeWaveColor = Color.White
            inactiveWaveColor = Color.White.copy(alpha = 0.35f)
            durationTextColor = Color.White.copy(alpha = 0.8f)
            buttonBgColor = Color.White.copy(alpha = 0.2f)
            buttonIconColor = Color.White
        } else {
            activeWaveColor = TealPrimaryDark
            inactiveWaveColor = TealPrimary.copy(alpha = 0.35f)
            durationTextColor = TealPrimaryDark
            buttonBgColor = TealPrimary.copy(alpha = 0.2f)
            buttonIconColor = TealPrimaryDark
        }
    } else {
        if (isDark) {
            activeWaveColor = TealMint
            inactiveWaveColor = TealDarkTextSecondary.copy(alpha = 0.35f)
            durationTextColor = TealDarkTextSecondary
            buttonBgColor = TealPrimary.copy(alpha = 0.25f)
            buttonIconColor = TealMint
        } else {
            activeWaveColor = TealPrimary
            inactiveWaveColor = TealLightDivider
            durationTextColor = TealLightTextSecondary
            buttonBgColor = TealPrimary.copy(alpha = 0.15f)
            buttonIconColor = TealPrimary
        }
    }

    Row(
        modifier = modifier
            .width(235.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Button in Teal
        IconButton(
            onClick = { handlePlayPause() },
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(buttonBgColor)
        ) {
            when (playbackState) {
                PlayerPlaybackState.PREPARING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = buttonIconColor
                    )
                }
                PlayerPlaybackState.PLAYING -> {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pausar",
                        tint = buttonIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = buttonIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Waveform visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                waveHeights.forEachIndexed { index, heightFactor ->
                    val barThreshold = index.toFloat() / waveHeights.size.toFloat()
                    val isPlayed = progress > barThreshold
                    val barColor = if (isPlayed) activeWaveColor else inactiveWaveColor

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((18.dp * heightFactor).coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Duration Display: "0:05 / 0:15" when playing, or "0:15" when idle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (playbackState == PlayerPlaybackState.PLAYING) {
                        "${formatDuration(currentPositionMs)} / ${formatDuration(durationMs)}"
                    } else {
                        if (durationMs > 0) formatDuration(durationMs) else "0:00"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = durationTextColor
                )

                Text(
                    text = if (playbackState == PlayerPlaybackState.PLAYING) "Reproduciendo" else "Nota de voz",
                    fontSize = 10.sp,
                    color = durationTextColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
