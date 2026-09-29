package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatEntity
import com.example.data.local.StatusEntity
import com.example.data.local.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.CreateStatusDialog
import com.example.ui.components.StatusStoriesBar
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.TelegramAvatar
import com.example.ui.components.UserProfileDialog
import com.example.ui.utils.TimeUtils
import com.example.ui.theme.CheckmarkBlue
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val chats by viewModel.filteredChats.collectAsState()
    val searchedContacts by viewModel.searchedContacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val inspectingUser by viewModel.inspectingUser.collectAsState()
    val activeStatuses by viewModel.activeStatuses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDark = LocalIsDarkTheme.current

    var showNewChatDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showCreateStatusDialog by remember { mutableStateOf(false) }
    var selectedStatusForViewer by remember { mutableStateOf<StatusEntity?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(TealPrimary, TealAccent, NexusViolet)
                                        )
                                    )
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "CP",
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CHATPRO",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Mensajería Segura",
                                    fontSize = 10.sp,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menú",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNewChatDialog = true },
                            modifier = Modifier.testTag("top_new_chat_button")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Nuevo Chat",
                                tint = TealPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Top Search Bar to quickly find conversations or contact names
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("chat_search_bar"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = if (searchQuery.isNotBlank()) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(TealPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_input")
                                .padding(vertical = 10.dp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isBlank()) {
                                    Text(
                                        text = "Buscar conversaciones o contactos...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.searchQuery.value = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Tabs: Todos, Privados, Grupos (shown when not searching)
                if (searchQuery.isBlank()) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = TealPrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { viewModel.selectedTab.value = 0 },
                            text = {
                                Text(
                                    "Todos",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { viewModel.selectedTab.value = 1 },
                            text = {
                                Text(
                                    "Privados",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { viewModel.selectedTab.value = 2 },
                            text = {
                                Text(
                                    "Grupos",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 2) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val isOwnerOrAdmin = currentUser?.role == "OWNER" || currentUser?.role == "ADMIN" ||
                    currentUser?.username.equals("Eliel_21", ignoreCase = true) ||
                    currentUser?.username.equals("admin", ignoreCase = true)

            Column(horizontalAlignment = Alignment.End) {
                if (isOwnerOrAdmin) {
                    AnimatedVisibility(visible = showFabMenu) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            // New Group Action (Only for Admin/Owner)
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                shadowElevation = 6.dp,
                                modifier = Modifier.clickable {
                                    showFabMenu = false
                                    showNewGroupDialog = true
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "Nuevo Grupo",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = TealPrimary)
                                }
                            }

                            // New Private Chat Action
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                shadowElevation = 6.dp,
                                modifier = Modifier.clickable {
                                    showFabMenu = false
                                    showNewChatDialog = true
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "Nuevo Chat Privado",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TealPrimary)
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = TealPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("new_chat_fab")
                    ) {
                        Icon(
                            imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "Nuevo Chat"
                        )
                    }
                } else {
                    // Regular users can only start direct chats
                    FloatingActionButton(
                        onClick = { showNewChatDialog = true },
                        containerColor = TealPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("new_chat_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Nuevo Chat Privado"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchQuery.isNotBlank()) {
                // --- SEARCH ACTIVE: Contacts + Conversations ---
                // 1. Matching Contacts Section
                if (searchedContacts.isNotEmpty()) {
                    item {
                        Text(
                            text = "CONTACTOS (${searchedContacts.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    items(searchedContacts, key = { "contact_${it.username}" }) { contact ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.createDirectChat(contact.username)
                                }
                                .padding(horizontal = 14.dp, vertical = 3.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.45f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TelegramAvatar(
                                    imageUrl = contact.avatarUrl,
                                    name = contact.displayName,
                                    size = 42.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${contact.username}",
                                        fontSize = 12.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Button(
                                    onClick = { viewModel.createDirectChat(contact.username) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chatear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 2. Matching Conversations Section
                if (chats.isNotEmpty()) {
                    item {
                        Text(
                            text = "CONVERSACIONES (${chats.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(chats, key = { it.id }) { chat ->
                        val isOwnerChat = chat.type == "DIRECT" && (
                            chat.id.equals("direct_eliel_21", ignoreCase = true) ||
                            chat.id.contains("___eliel_21", ignoreCase = true) ||
                            chat.id.contains("eliel_21___", ignoreCase = true)
                        )

                        ChatItemRow(
                            chat = chat,
                            currentUsername = currentUser?.username,
                            isOwnerChat = isOwnerChat,
                            onClick = { viewModel.navigateTo(Screen.ChatDetail(chat.id)) },
                            onAvatarClick = {
                                if (chat.type == "DIRECT") {
                                    val username = chat.id.removePrefix("direct_")
                                    viewModel.inspectUser(username)
                                } else {
                                    viewModel.navigateTo(Screen.ChatDetail(chat.id))
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                } else if (searchedContacts.isEmpty()) {
                    // Both empty
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Sin resultados para \"$searchQuery\"",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "No se encontraron conversaciones ni contactos con ese término.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { viewModel.searchQuery.value = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Limpiar búsqueda")
                                }
                            }
                        }
                    }
                }
            } else {
                // --- NORMAL LIST MODE ---
                // Stories / Estados bar
                item {
                    StatusStoriesBar(
                        statuses = activeStatuses,
                        currentUser = currentUser,
                        onAddStatusClick = { showCreateStatusDialog = true },
                        onStatusClick = { status -> selectedStatusForViewer = status }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }

                if (chats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(TealPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = TealPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Sin conversaciones aún",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Inicia un chat privado o escribe en la comunidad para comenzar.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = { showNewChatDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("btn_empty_start_chat")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Iniciar primer chat", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(chats, key = { it.id }) { chat ->
                        val isOwnerChat = chat.type == "DIRECT" && (
                            chat.id.equals("direct_eliel_21", ignoreCase = true) ||
                            chat.id.contains("___eliel_21", ignoreCase = true) ||
                            chat.id.contains("eliel_21___", ignoreCase = true)
                        )

                        ChatItemRow(
                            chat = chat,
                            currentUsername = currentUser?.username,
                            isOwnerChat = isOwnerChat,
                            onClick = { viewModel.navigateTo(Screen.ChatDetail(chat.id)) },
                            onAvatarClick = {
                                if (chat.type == "DIRECT") {
                                    val username = chat.id.removePrefix("direct_")
                                    viewModel.inspectUser(username)
                                } else {
                                    viewModel.navigateTo(Screen.ChatDetail(chat.id))
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    // Inspecting User Modal Dialog
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

    // Create 24h Status Dialog (Photo, Video & Text)
    if (showCreateStatusDialog) {
        CreateStatusDialog(
            currentUser = currentUser,
            onDismiss = { showCreateStatusDialog = false },
            onPublishText = { text, colorHex ->
                viewModel.publishStatusText(text, colorHex) {
                    showCreateStatusDialog = false
                }
            },
            onPublishMedia = { text, uri, isVideo ->
                viewModel.publishMediaStatus(text, uri, isVideo) {
                    showCreateStatusDialog = false
                }
            }
        )
    }

    // Story / Status Viewer Modal
    if (selectedStatusForViewer != null) {
        val status = selectedStatusForViewer!!
        StoryViewerDialog(
            status = status,
            currentUsername = currentUser?.username,
            isAdmin = currentUser?.role == "OWNER" || currentUser?.username == "Eliel_21",
            onDismiss = { selectedStatusForViewer = null },
            onDelete = {
                viewModel.deleteStatus(status.id)
                selectedStatusForViewer = null
            }
        )
    }

    // New Direct Chat Dialog WITH REAL USER VERIFICATION
    if (showNewChatDialog) {
        var usernameInput by remember { mutableStateOf("") }
        var foundUser by remember { mutableStateOf<UserEntity?>(null) }
        var isSearching by remember { mutableStateOf(false) }
        var directoryUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
        val clean = usernameInput.trim().removePrefix("@")

        LaunchedEffect(Unit) {
            directoryUsers = viewModel.getAllDirectoryUsers()
        }

        // Live verify if user exists
        LaunchedEffect(clean) {
            if (clean.length >= 3) {
                isSearching = true
                foundUser = viewModel.checkUserExists(clean)
                isSearching = false
            } else {
                foundUser = null
            }
        }

        val canStart = foundUser != null && !foundUser!!.isCurrentUser

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nuevo Chat Privado", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Introduce el nombre de usuario (@) del contacto. El sistema verificará que exista en ChatPro:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it.replace(" ", "") },
                        label = { Text("Nombre de Usuario (@)") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = TealPrimary) },
                        trailingIcon = {
                            when {
                                clean.length < 3 -> {}
                                foundUser != null -> {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verificado", tint = OnlineGreen)
                                }
                                else -> {
                                    Icon(Icons.Default.Error, contentDescription = "No existe", tint = ErrorRed)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_user_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (foundUser != null) OnlineGreen else if (clean.length >= 3) ErrorRed else TealPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Real-time user verification card
                    Spacer(modifier = Modifier.height(10.dp))
                    when {
                        foundUser != null -> {
                            val isEliel = foundUser!!.username.equals("Eliel_21", ignoreCase = true)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEliel) NexusGold else OnlineGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TelegramAvatar(
                                        imageUrl = foundUser!!.avatarUrl,
                                        name = foundUser!!.displayName,
                                        size = 38.dp,
                                        showOnlineDot = foundUser!!.isCurrentUser
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = foundUser!!.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isEliel) {
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
                                        Text(
                                            text = "✓ Usuario encontrado en ChatPro",
                                            fontSize = 11.sp,
                                            color = OnlineGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        clean.length >= 3 -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ErrorRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "El usuario @$clean no existe en ChatPro.",
                                        fontSize = 11.sp,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canStart) {
                            viewModel.createDirectChat(clean)
                            showNewChatDialog = false
                        }
                    },
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("confirm_new_chat_button")
                ) {
                    Text("Iniciar Chat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // New Group Dialog WITH MEMBER SELECTION AND PHOTO
    if (showNewGroupDialog) {
        var groupTitle by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var groupAvatarUrl by remember { mutableStateOf("") }
        var directoryUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
        val selectedMembers = remember { mutableStateListOf<String>() }

        val groupPhotoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                groupAvatarUrl = uri.toString()
            }
        }

        LaunchedEffect(Unit) {
            directoryUsers = viewModel.getAllDirectoryUsers()
        }

        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = NexusViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Nuevo Grupo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Group Photo Avatar Picker
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                groupPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (groupAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = groupAvatarUrl,
                                contentDescription = "Foto del grupo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Image, contentDescription = "Elegir foto", tint = TealPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text(
                        text = if (groupAvatarUrl.isNotBlank()) "Foto elegida ✓ (toca para cambiar)" else "Toca para añadir foto al grupo",
                        fontSize = 11.sp,
                        color = if (groupAvatarUrl.isNotBlank()) OnlineGreen else TealPrimary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it },
                        label = { Text("Nombre del Grupo *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_group_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Descripción o Tema (opcional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (directoryUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Añadir miembros iniciales:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                            items(directoryUsers) { u ->
                                val isSelected = u.username in selectedMembers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedMembers.remove(u.username)
                                            else selectedMembers.add(u.username)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TelegramAvatar(
                                        imageUrl = u.avatarUrl,
                                        name = u.displayName,
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = u.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(text = "@${u.username}", fontSize = 11.sp, color = TealPrimary)
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedMembers.add(u.username)
                                            else selectedMembers.remove(u.username)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupTitle.isNotBlank()) {
                            viewModel.createGroup(groupTitle.trim(), groupDesc.trim(), groupAvatarUrl, selectedMembers.toList())
                            showNewGroupDialog = false
                        }
                    },
                    enabled = groupTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("confirm_new_group_button")
                ) {
                    Text("Crear Grupo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ChatItemRow(
    chat: ChatEntity,
    currentUsername: String? = null,
    isOwnerChat: Boolean = false,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val formattedDate = remember(chat.lastMessageTime) {
        TimeUtils.formatChatListDate(chat.lastMessageTime)
    }

    // Determine if the last message was sent by the current active user
    val isSentByMe = !currentUsername.isNullOrBlank() &&
            chat.lastMessageSenderId.isNotBlank() &&
            chat.lastMessageSenderId.equals(currentUsername, ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        TelegramAvatar(
            imageUrl = chat.avatarUrl,
            name = chat.title,
            size = 52.dp,
            isGroup = chat.type == "GROUP",
            showOnlineDot = false,
            onClick = onAvatarClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Center: Title & Snippet
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (chat.type == "GROUP") {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = NexusViolet,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = chat.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Only show outgoing delivery checkmark IF the last message was sent by the current user
                    if (isSentByMe) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Enviado por ti",
                            tint = CheckmarkBlue,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    Text(
                        text = chat.lastMessageSnippet.ifBlank { "Toca para abrir la conversación" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.pinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Fijado",
                            tint = TealPrimary,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(end = 6.dp)
                        )
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TealPrimary)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
