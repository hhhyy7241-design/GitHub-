package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Verified
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.TelegramAvatar
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.launch

@Composable
fun ProfileDrawerContent(
    viewModel: MainViewModel,
    onCloseDrawer: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val isDark = LocalIsDarkTheme.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDirectoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var localAccounts by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var directoryUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val isOwner = currentUser?.username?.equals("Eliel_21", ignoreCase = true) == true

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            if (isDark) {
                                listOf(
                                    if (isOwner) Color(0xFF2E2005) else MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            } else {
                                listOf(
                                    if (isOwner) NexusGold.copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.surface
                                )
                            }
                        )
                    )
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // User Avatar with Ring
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        if (isOwner) listOf(NexusGold, Color(0xFFFF6F00), NexusGold)
                                        else listOf(TealPrimary, TealAccent, NexusViolet, TealPrimary)
                                    )
                                )
                                .padding(2.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TelegramAvatar(
                                imageUrl = currentUser?.avatarUrl,
                                name = currentUser?.displayName ?: "Usuario",
                                size = 63.dp,
                                showOnlineDot = true
                            )
                        }

                        if (isOwner) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NexusGold.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, NexusGold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "creador",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) NexusGold else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentUser?.displayName ?: "Usuario",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOwner) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = NexusGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "@${currentUser?.username ?: "usuario"}",
                        fontSize = 13.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!currentUser?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentUser?.bio ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OnlineGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "En línea • ChatPro",
                            fontSize = 11.sp,
                            color = OnlineGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Items
            NavigationDrawerItem(
                label = { Text("Mis Conversaciones", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Message, contentDescription = null, tint = TealPrimary) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.ChatList)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Directorio de Usuarios", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.People, contentDescription = null, tint = NexusViolet) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    coroutineScope.launch {
                        directoryUsers = viewModel.getAllDirectoryUsers()
                        showDirectoryDialog = true
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Editar Mi Perfil (@usuario)", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    showEditProfileDialog = true
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
            )

            // Theme Switcher Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tema de la aplicación",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Dark option
                        val isOptionDark = currentThemeMode == AppThemeMode.DARK
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOptionDark) TealPrimary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setThemeMode(AppThemeMode.DARK) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = if (isOptionDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Oscuro",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOptionDark) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOptionDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Light option
                        val isOptionLight = currentThemeMode == AppThemeMode.LIGHT
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOptionLight) TealPrimary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setThemeMode(AppThemeMode.LIGHT) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (isOptionLight) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Claro",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOptionLight) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOptionLight) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Auto option
                        val isOptionAuto = currentThemeMode == AppThemeMode.SYSTEM
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOptionAuto) TealPrimary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setThemeMode(AppThemeMode.SYSTEM) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.BrightnessAuto,
                                    contentDescription = null,
                                    tint = if (isOptionAuto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Auto",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOptionAuto) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOptionAuto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            NavigationDrawerItem(
                label = { Text("Acerca de ChatPro", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    showAboutDialog = true
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Cambiar de Cuenta", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = NexusGold) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    coroutineScope.launch {
                        localAccounts = viewModel.getAllLocalAccounts()
                        showSwitchAccountDialog = true
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    viewModel.logout()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Eliminar Mi Cuenta", color = ErrorRed) },
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    showDeleteAccountDialog = true
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Edit Profile Modal Dialog
    if (showEditProfileDialog) {
        var newDisplayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
        var newBio by remember { mutableStateOf(currentUser?.bio ?: "") }
        var newAvatarUrl by remember { mutableStateOf(currentUser?.avatarUrl ?: "") }
        var isSaving by remember { mutableStateOf(false) }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                newAvatarUrl = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
            title = {
                Text(
                    text = "Editar Mi Perfil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        TelegramAvatar(
                            imageUrl = newAvatarUrl,
                            name = newDisplayName.ifBlank { "U" },
                            size = 76.dp
                        )
                    }

                    Text(
                        text = "Toca para cambiar foto",
                        fontSize = 11.sp,
                        color = TealPrimary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text("Nombre visible") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Biografía / Estado") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nombre de usuario: @${currentUser?.username} (fijo)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        viewModel.updateProfile(
                            displayName = newDisplayName.trim(),
                            bio = newBio.trim(),
                            avatarUrl = newAvatarUrl
                        )
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Directory of Users Modal
    if (showDirectoryDialog) {
        AlertDialog(
            onDismissRequest = { showDirectoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = NexusViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Directorio de Usuarios", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Usuarios registrados en ChatPro. Toca para ver su perfil o escribirle un mensaje directo:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (directoryUsers.isEmpty()) {
                        Text(
                            text = "No se encontraron otros usuarios en el directorio.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(directoryUsers) { u ->
                                val isEliel = u.username.equals("Eliel_21", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            showDirectoryDialog = false
                                            viewModel.inspectUserEntity(u)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TelegramAvatar(
                                            imageUrl = u.avatarUrl,
                                            name = u.displayName,
                                            size = 40.dp,
                                            showOnlineDot = u.isCurrentUser
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = u.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
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
                                                text = "@${u.username}",
                                                fontSize = 11.sp,
                                                color = TealPrimary
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Chat",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDirectoryDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("Acerca de ChatPro", fontWeight = FontWeight.Black)
            },
            text = {
                Column {
                    Text("ChatPro Messenger", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Versión 2.0 • Mensajería Instantánea y Almacenamiento Local", fontSize = 12.sp, color = TealPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NexusGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "👑 Creador y Desarrollador:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDark) NexusGold else Color(0xFFB45309)
                            )
                            Text(
                                text = "Eliel (@Eliel_21)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sistema de mensajería rápida con intercambio estructurado JSON y almacenamiento local en Room Database.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Entendido", color = Color.White)
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿Eliminar Cuenta?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Esta acción eliminará permanentemente tu perfil (@${currentUser?.username}), tus estados e información del servidor. ¿Deseas continuar?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Eliminar definitivamente", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Switch Account Dialog
    if (showSwitchAccountDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = NexusGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cambiar de Cuenta", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Cuentas guardadas en este dispositivo. Toca una para cambiar:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(localAccounts, key = { it.username }) { account ->
                            val isCurrent = account.username.equals(currentUser?.username, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, TealPrimary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        if (!isCurrent) {
                                            viewModel.switchAccount(account.username)
                                            showSwitchAccountDialog = false
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TelegramAvatar(imageUrl = account.avatarUrl, name = account.displayName, size = 38.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(account.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("(Activa)", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("@${account.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!isCurrent) {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteSpecificAccount(account.username)
                                                coroutineScope.launch {
                                                    localAccounts = viewModel.getAllLocalAccounts()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Eliminar cuenta guardada",
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            showSwitchAccountDialog = false
                            viewModel.logout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar sesión con otra cuenta")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwitchAccountDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
