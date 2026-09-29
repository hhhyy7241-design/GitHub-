package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.StatusEntity
import com.example.data.local.UserEntity
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NexusGold
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusStoriesBar(
    statuses: List<StatusEntity>,
    currentUser: UserEntity?,
    onAddStatusClick: () -> Unit,
    onStatusClick: (StatusEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val myStatuses = statuses.filter { it.authorUsername == currentUser?.username }
    val otherStatuses = statuses.filter { it.authorUsername != currentUser?.username }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ESTADOS DE 24 HORAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "Fotos, Videos y Texto",
                fontSize = 10.sp,
                color = TealPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // My status slot
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(68.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                if (myStatuses.isNotEmpty()) {
                                    onStatusClick(myStatuses.first())
                                } else {
                                    onAddStatusClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (myStatuses.isNotEmpty()) {
                            // Ring around my active status
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(TealPrimary, TealAccent, OnlineGreen, TealPrimary)
                                        )
                                    )
                                    .padding(2.dp)
                            )
                        }

                        TelegramAvatar(
                            imageUrl = currentUser?.avatarUrl,
                            name = currentUser?.displayName ?: "Yo",
                            size = 50.dp,
                            showOnlineDot = false
                        )

                        // Floating plus badge on bottom right
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(TealPrimary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable { onAddStatusClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Añadir estado",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mi Estado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Other contacts' active statuses
            items(otherStatuses, key = { it.id }) { status ->
                val isCreator = status.authorUsername.equals("Eliel_21", ignoreCase = true)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onStatusClick(status) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    if (isCreator) {
                                        listOf(NexusGold, Color(0xFFFF8F00), NexusGold)
                                    } else {
                                        listOf(TealPrimary, TealAccent, OnlineGreen, TealPrimary)
                                    }
                                )
                            )
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TelegramAvatar(
                            imageUrl = status.authorAvatar,
                            name = status.authorDisplayName,
                            size = 49.dp,
                            showOnlineDot = false
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status.authorDisplayName.split(" ").firstOrNull() ?: status.authorDisplayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CreateStatusDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onPublishText: (text: String, colorHex: String) -> Unit,
    onPublishMedia: (text: String, uri: Uri, isVideo: Boolean) -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0D9488") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isVideoMedia by remember { mutableStateOf(false) }

    val colorOptions = listOf(
        "#0D9488" to "Teal",
        "#0284C7" to "Azul",
        "#7C3AED" to "Violeta",
        "#E11D48" to "Carmesí",
        "#059669" to "Esmeralda",
        "#0F172A" to "Noche"
    )

    val imageMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            isVideoMedia = false
        }
    }

    val videoMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            isVideoMedia = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Publicar Estado (24h)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Comparte fotos, videos o textos con la comunidad nacional en Moodle.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    placeholder = { Text("Escribe un mensaje o pie de foto...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Color de fondo (para textos):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Photos & Videos Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pick Photo
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                imageMediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedMediaUri != null && !isVideoMedia) "Foto ✓" else "Foto",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedMediaUri != null && !isVideoMedia) OnlineGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Pick Video
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                videoMediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedMediaUri != null && isVideoMedia) "Video ✓" else "Video",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedMediaUri != null && isVideoMedia) OnlineGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedMediaUri != null) {
                        onPublishMedia(statusText.trim(), selectedMediaUri!!, isVideoMedia)
                    } else if (statusText.isNotBlank()) {
                        onPublishText(statusText.trim(), selectedColor)
                    }
                },
                enabled = statusText.isNotBlank() || selectedMediaUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Publicar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun StoryViewerDialog(
    status: StatusEntity,
    currentUsername: String?,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(status.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6500, easing = LinearEasing)
        )
        onDismiss()
    }

    val context = LocalContext.current
    val parsedColor = remember(status.backgroundColorHex) {
        try {
            Color(android.graphics.Color.parseColor(status.backgroundColorHex))
        } catch (e: Exception) {
            Color(0xFF0D9488)
        }
    }

    val formattedTime = remember(status.createdAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(status.createdAt))
    }

    val isMyStatus = status.authorUsername == currentUsername || isAdmin

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (!status.mediaUrl.isNullOrBlank()) Color.Black else parsedColor)
        ) {
            // Media Image or Background
            if (!status.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(status.mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Estado",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Status Bar: Progress & User Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.35f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TelegramAvatar(
                            imageUrl = status.authorAvatar,
                            name = status.authorDisplayName,
                            size = 38.dp,
                            showOnlineDot = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = status.authorDisplayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Hoy a las $formattedTime",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMyStatus) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }
            }

            // Text Caption / Centered Text
            if (status.text.isNotBlank()) {
                if (status.mediaUrl.isNullOrBlank()) {
                    // Fullscreen bold message
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status.text,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                } else {
                    // Bottom Caption Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status.text,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
