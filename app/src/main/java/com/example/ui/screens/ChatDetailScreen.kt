package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ui.components.AudioRecorderHelper
import com.example.ui.components.AudioMessageBubble
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.MessageEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GroupInfoDialog
import com.example.ui.components.TelegramAvatar
import com.example.ui.components.UserProfileDialog
import com.example.ui.utils.TimeUtils
import com.example.ui.theme.CheckmarkBlue
import com.example.ui.theme.CheckmarkTeal
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealDarkIncomingBubble
import com.example.ui.theme.TealDarkOutgoingBubble
import com.example.ui.theme.TealLightIncomingBubble
import com.example.ui.theme.TealLightOutgoingBubble
import com.example.ui.theme.TealLightTextPrimary
import com.example.ui.theme.TealLightTextSecondary
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TelegramDarkIncomingBubble
import com.example.ui.theme.TelegramDarkOutgoingBubble
import com.example.ui.theme.TelegramLightIncomingBubble
import com.example.ui.theme.TelegramLightOutgoingBubble
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeChat by viewModel.activeChat.collectAsState()
    val activeInterlocutor by viewModel.activeInterlocutor.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val activeGroupMembers by viewModel.activeGroupMembers.collectAsState()
    val inspectingUser by viewModel.inspectingUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showChatInfoDialog by remember { mutableStateOf(false) }
    var selectedImageForViewer by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var selectedMessageForOptions by remember { mutableStateOf<MessageEntity?>(null) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Google Play compliant Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendAttachment(uri, isImage = true)
        }
    }

    // Document Picker for files <= 4MB
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendAttachment(uri, isImage = false)
        }
    }

    val audioRecorderHelper = remember { AudioRecorderHelper(context) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = audioRecorderHelper.startRecording()
            if (file != null) {
                isRecordingAudio = true
                recordingDurationSeconds = 0
            }
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            while (isRecordingAudio) {
                kotlinx.coroutines.delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    val isDark = LocalIsDarkTheme.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    val isGroup = activeChat?.type == "GROUP"
                    val isOwnerChat = activeInterlocutor?.username?.equals("Eliel_21", ignoreCase = true) == true ||
                            activeInterlocutor?.displayName?.contains("Eliel", ignoreCase = true) == true ||
                            activeChat?.id?.contains("Eliel_21", ignoreCase = true) == true

                    val displayTitle = if (isGroup) {
                        activeChat?.title ?: "Grupo"
                    } else {
                        activeInterlocutor?.displayName ?: activeChat?.title ?: "Chat"
                    }

                    val displayAvatar = if (isGroup) {
                        activeChat?.avatarUrl
                    } else {
                        activeInterlocutor?.avatarUrl ?: activeChat?.avatarUrl
                    }

                    val subtitle = if (isGroup) {
                        activeChat?.description?.takeIf { it.isNotBlank() } ?: "${activeGroupMembers.size} miembros"
                    } else {
                        val lastSeenTime = activeInterlocutor?.lastSeen ?: activeChat?.lastMessageTime ?: 0L
                        val isOnline = activeInterlocutor?.isCurrentUser == true
                        TimeUtils.formatLastSeen(lastSeenTime, isOnline)
                    }

                    val isOnlineStatus = !isGroup && subtitle == "en línea"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (isGroup) {
                                    showChatInfoDialog = true
                                } else if (activeInterlocutor != null) {
                                    viewModel.inspectUserEntity(activeInterlocutor!!)
                                } else {
                                    val raw = activeChat?.id?.removePrefix("dm_")?.removePrefix("direct_") ?: ""
                                    val participants = raw.split("_").map { it.trim().removePrefix("@") }
                                    val cur = currentUser?.username ?: ""
                                    val other = participants.firstOrNull { !it.equals(cur, ignoreCase = true) } ?: participants.firstOrNull() ?: ""
                                    if (other.isNotBlank()) {
                                        viewModel.inspectUser(other)
                                    }
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        TelegramAvatar(
                            imageUrl = displayAvatar,
                            name = displayTitle,
                            size = 42.dp,
                            isGroup = isGroup,
                            showOnlineDot = isOnlineStatus
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isOwnerChat) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NexusGold.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NexusGold.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "creador",
                                            color = if (isDark) NexusGold else Color(0xFFB45309),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOnlineStatus) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(OnlineGreen)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = if (isOnlineStatus) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isOnlineStatus) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Información del Chat") },
                                onClick = {
                                    menuExpanded = false
                                    showChatInfoDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TealPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Vaciar chat (Borrar historial local)") },
                                onClick = {
                                    menuExpanded = false
                                    showClearChatDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ErrorRed) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Upload Progress Banner
                AnimatedVisibility(visible = uploadProgress.isUploading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = NexusPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Subiendo archivo...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${uploadProgress.percentage}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress.percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = NexusPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uploadProgress.fileName} (${String.format(Locale.US, "%.1f", uploadProgress.totalBytes / (1024.0 * 1024.0))} MB)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Input Bar or Audio Recording Bar
                if (isRecordingAudio) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grabando: ${String.format("%02d:%02d", recordingDurationSeconds / 60, recordingDurationSeconds % 60)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                audioRecorderHelper.cancelRecording()
                                isRecordingAudio = false
                                recordingDurationSeconds = 0
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = ErrorRed)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val file = audioRecorderHelper.stopRecording()
                                isRecordingAudio = false
                                recordingDurationSeconds = 0
                                if (file != null && file.exists() && file.length() > 50 && activeChat != null) {
                                    viewModel.sendAudioMessage(activeChat!!.id, file)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OnlineGreen)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar nota", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Replying Header Banner
                        if (replyingToMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(30.dp)
                                            .background(TealAccent, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Respondiendo a ${replyingToMessage?.senderName ?: "usuario"}",
                                            color = TealAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = replyingToMessage?.text?.takeIf { it.isNotBlank() } ?: (replyingToMessage?.attachmentName ?: "Archivo adjunto"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { replyingToMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Editing Header Banner
                        if (editingMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(30.dp)
                                            .background(OnlineGreen, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Editando mensaje",
                                            color = OnlineGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = editingMessage?.text ?: "",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingMessage = null
                                            messageInput = ""
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAttachmentSheet = true },
                                modifier = Modifier.size(38.dp).testTag("attach_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Adjuntar archivo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.65f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = messageInput,
                                    onValueChange = {
                                        if (it.length <= 1000) {
                                            messageInput = it
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("message_input_field"),
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Default
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (messageInput.isEmpty()) {
                                            Text(
                                                text = if (editingMessage != null) "Edita tu mensaje..." else "Escribe un mensaje...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                if (messageInput.length >= 800) {
                                    Text(
                                        text = "${messageInput.length}/1000",
                                        fontSize = 9.sp,
                                        color = if (messageInput.length >= 950) ErrorRed else TealPrimary,
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            if (messageInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val textToSend = messageInput.trim().take(1000)
                                        if (textToSend.isNotBlank()) {
                                            if (editingMessage != null) {
                                                viewModel.editMessage(editingMessage!!.id, textToSend)
                                                editingMessage = null
                                                messageInput = ""
                                            } else if (replyingToMessage != null) {
                                                viewModel.sendMessage(
                                                    text = textToSend,
                                                    replyToId = replyingToMessage!!.id,
                                                    replyToSender = replyingToMessage!!.senderName,
                                                    replyToText = replyingToMessage!!.text.takeIf { it.isNotBlank() } ?: (replyingToMessage!!.attachmentName ?: "Archivo")
                                                )
                                                replyingToMessage = null
                                                messageInput = ""
                                            } else {
                                                viewModel.sendMessage(textToSend)
                                                messageInput = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (editingMessage != null) OnlineGreen else TealPrimary)
                                        .testTag("send_button")
                                ) {
                                    Icon(
                                        imageVector = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                        contentDescription = if (editingMessage != null) "Guardar edición" else "Enviar",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        )
                                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            val file = audioRecorderHelper.startRecording()
                                            if (file != null) {
                                                isRecordingAudio = true
                                                recordingDurationSeconds = 0
                                            }
                                        } else {
                                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TealPrimary)
                                        .testTag("record_audio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Grabar audio",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "💬 Conversación activa en Nexus",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay mensajes en esta conversación.\nEscribe el primer mensaje para comenzar.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                TelegramMessageBubble(
                    message = message,
                    currentUsername = currentUser?.username,
                    isGroup = activeChat?.type == "GROUP",
                    onImageClick = { url -> selectedImageForViewer = url },
                    onCopyLink = { url ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Enlace", url)
                        clipboard.setPrimaryClip(clip)
                    },
                    onOpenBrowser = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (ignored: Exception) {}
                    },
                    onSenderClick = { senderId ->
                        viewModel.inspectUser(senderId)
                    },
                    onBubbleClick = {
                        selectedMessageForOptions = message
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Compartir Multimedia",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Límite máximo por archivo: 4.0 MB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Gallery Photo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = NexusPrimary)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Foto de Galería", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Envía una foto de alta calidad (≤ 4.0 MB)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // File/Document
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentSheet = false
                            filePickerLauncher.launch("*/*")
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8F51E8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = Color(0xFF8F51E8))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Archivo / Documento", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("PDF, ZIP, APK, Word (≤ 4.0 MB)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Group Info & Member Management Dialog
    if (showChatInfoDialog && activeChat != null && activeChat?.type == "GROUP") {
        val chat = activeChat!!
        GroupInfoDialog(
            chat = chat,
            members = activeGroupMembers,
            currentUser = currentUser,
            onDismiss = { showChatInfoDialog = false },
            onInspectMember = { user ->
                viewModel.inspectUserEntity(user)
            },
            onAddMember = { username ->
                viewModel.addMemberToGroup(chat.id, username)
            },
            onRemoveMember = { username ->
                viewModel.removeMemberFromGroup(chat.id, username)
            },
            onChangeRole = { username, newRole ->
                viewModel.updateMemberRole(chat.id, username, newRole)
            },
            onLoadDirectoryUsers = {
                viewModel.getAllDirectoryUsers()
            }
        )
    }

    // Interactive User Profile Dialog
    if (inspectingUser != null) {
        val isMe = inspectingUser?.username?.equals(currentUser?.username, ignoreCase = true) == true
        val isCurrentAdmin = currentUser?.username?.equals("Eliel_21", ignoreCase = true) == true

        UserProfileDialog(
            user = inspectingUser!!,
            isCurrentUser = isMe,
            canBan = !isMe && isCurrentAdmin,
            onDismiss = { viewModel.closeUserInspection() },
            onStartDirectChat = {
                viewModel.openDirectChatWithInspectedUser()
            },
            onBanUserDevice = { username ->
                viewModel.banUserAndDevice(username)
            }
        )
    }

    // Fullscreen Image Viewer
    if (selectedImageForViewer != null) {
        Dialog(
            onDismissRequest = { selectedImageForViewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selectedImageForViewer)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { selectedImageForViewer = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Message Contextual Options Dialog (Reply, Edit, Copy, Delete Attachment, Delete Message)
    if (selectedMessageForOptions != null) {
        val msg = selectedMessageForOptions!!
        val isAuthor = msg.senderId.equals(currentUser?.username, ignoreCase = true)
        val isAdmin = currentUser?.role == "OWNER" || currentUser?.username?.equals("Eliel_21", ignoreCase = true) == true
        val canEdit = isAuthor || isAdmin
        val canDelete = isAuthor || isAdmin

        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = {
                Text(text = "Opciones del Mensaje", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Responder
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                replyingToMessage = msg
                                editingMessage = null
                                selectedMessageForOptions = null
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Responder", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Editar (if author or admin and message has text)
                    if (canEdit && msg.text.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingMessage = msg
                                    replyingToMessage = null
                                    messageInput = msg.text
                                    selectedMessageForOptions = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = OnlineGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Editar mensaje", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 3. Copiar texto
                    if (msg.text.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Mensaje", msg.text))
                                    selectedMessageForOptions = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Copiar texto", fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 4. Borrar archivo adjunto
                    if (!msg.attachmentUrl.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.deleteAttachment(msg.id)
                                    selectedMessageForOptions = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = NexusGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Borrar archivo adjunto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Libera espacio local en tu dispositivo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 5. Eliminar mensaje
                    if (canDelete) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ErrorRed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.deleteMessage(msg.id)
                                    selectedMessageForOptions = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Eliminar mensaje", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed)
                                    Text(
                                        if (isAdmin && !isAuthor) "Moderación de Administrador (@Eliel_21)" else "Se borra de la base de datos Room local",
                                        fontSize = 11.sp,
                                        color = ErrorRed.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedMessageForOptions = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Confirm Clear Chat Dialog
    if (showClearChatDialog && activeChat != null) {
        val chat = activeChat!!
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = {
                Text("Vaciar historial del chat", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas vaciar todos los mensajes de \"${chat.title}\"? Se borrarán de la base de datos Room de tu teléfono para liberar almacenamiento.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat(chat.id)
                        showClearChatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Vaciar Chat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TelegramMessageBubble(
    message: MessageEntity,
    currentUsername: String? = null,
    isGroup: Boolean,
    onImageClick: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    onSenderClick: ((String) -> Unit)? = null,
    onBubbleClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val isOutgoing = if (!currentUsername.isNullOrBlank()) {
        message.senderId.equals(currentUsername, ignoreCase = true)
    } else {
        message.isOutgoing
    }
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (isOutgoing) {
        if (isDark) TelegramDarkOutgoingBubble else TelegramLightOutgoingBubble
    } else {
        if (isDark) TelegramDarkIncomingBubble else MaterialTheme.colorScheme.surface
    }
    val messageTextColor = if (isOutgoing) {
        if (isDark) Color.White else TealLightTextPrimary
    } else {
        if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    }
    val messageMetaColor = if (isOutgoing) {
        if (isDark) Color.White.copy(alpha = 0.65f) else TealLightTextSecondary
    } else {
        if (isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }

    val formattedTime = remember(message.timestamp) {
        TimeUtils.formatMessageTime(message.timestamp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOutgoing) {
            TelegramAvatar(
                imageUrl = message.senderAvatar,
                name = message.senderName,
                size = 32.dp,
                onClick = { onSenderClick?.invoke(message.senderId) }
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 300.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .then(
                    if (onBubbleClick != null) Modifier.clickable { onBubbleClick() } else Modifier
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                // Sender Name and Admin/Owner Verification Badge (ONLY for Eliel_21)
                val isOwnerMessage = message.senderId.equals("Eliel_21", ignoreCase = true)

                if (!isOutgoing && (isGroup || isOwnerMessage)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onSenderClick?.invoke(message.senderId) }
                            .padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = message.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOwnerMessage) NexusGold else TealPrimary
                        )
                        if (isOwnerMessage) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NexusGold.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, NexusGold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "creador",
                                    color = if (isDark) NexusGold else Color(0xFFB45309),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }
                }

                // Reply Quote Preview inside Bubble
                if (!message.replyToText.isNullOrBlank()) {
                    Surface(
                        color = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .drawBehind {
                                drawLine(
                                    color = TealAccent,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 6f
                                )
                            }
                            .padding(start = 8.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyToSender ?: "Mensaje",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent,
                                maxLines = 1
                            )
                            Text(
                                text = message.replyToText,
                                fontSize = 11.sp,
                                color = messageTextColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Audio Note Attachment
                if (message.attachmentType == "AUDIO" && !message.attachmentUrl.isNullOrEmpty()) {
                    AudioMessageBubble(
                        audioUrlOrPath = message.attachmentUrl,
                        attachmentName = message.attachmentName,
                        isOutgoing = isOutgoing
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Image Attachment
                if (message.attachmentType == "IMAGE" && !message.attachmentUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(message.attachmentUrl) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(message.attachmentUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = message.attachmentName ?: "Foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        if (message.attachmentSize > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", message.attachmentSize / (1024.0 * 1024.0))} MB",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Document / File Attachment (<= 4MB)
                if (message.attachmentType == "DOCUMENT" || (message.attachmentUrl != null && message.attachmentType != "IMAGE" && message.attachmentType != "AUDIO")) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NexusPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.attachmentName ?: "Archivo adjunto",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sizeText = if (message.attachmentSize > 0) {
                                    "${String.format(Locale.US, "%.2f", message.attachmentSize / (1024.0 * 1024.0))} MB"
                                } else {
                                    "Documento"
                                }
                                Text(
                                    text = sizeText,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Copy Link / Open Button
                            IconButton(
                                onClick = { message.attachmentUrl?.let { onCopyLink(it) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar Enlace",
                                    modifier = Modifier.size(16.dp),
                                    tint = NexusPrimary
                                )
                            }
                            IconButton(
                                onClick = { message.attachmentUrl?.let { onOpenBrowser(it) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Abrir Enlace",
                                    modifier = Modifier.size(16.dp),
                                    tint = NexusPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Text Content
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = messageTextColor
                    )
                }

                // Timestamp, Edited Badge and Delivery Status
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "editado ",
                            fontSize = 10.sp,
                            color = messageMetaColor.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = messageMetaColor
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            "PENDING" -> Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Enviando",
                                tint = messageMetaColor,
                                modifier = Modifier.size(12.dp)
                            )
                            "READ" -> Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Leído",
                                tint = if (isDark) TealAccent else Color(0xFF0284C7),
                                modifier = Modifier.size(15.dp)
                            )
                            "DELIVERED" -> Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Entregado en Moodle",
                                tint = messageMetaColor,
                                modifier = Modifier.size(15.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Enviado",
                                tint = messageMetaColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
