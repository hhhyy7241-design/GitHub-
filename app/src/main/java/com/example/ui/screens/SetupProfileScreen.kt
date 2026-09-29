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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary

@Composable
fun SetupProfileScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // Tab state: 0 = Iniciar Sesión, 1 = Crear Cuenta
    var selectedTab by remember { mutableIntStateOf(0) }

    // Login Fields
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Register Fields
    var registerUsername by remember { mutableStateOf("") }
    var registerDisplayName by remember { mutableStateOf("") }
    var registerBio by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var showRegisterPassword by remember { mutableStateOf(false) }
    var avatarUriString by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameFeedback by remember { mutableStateOf<String?>(null) }

    val cleanRegisterUsername = registerUsername.trim().removePrefix("@")

    // Image Picker for registration avatar
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUriString = uri.toString()
        }
    }

    // Live check if username is available during registration
    LaunchedEffect(cleanRegisterUsername) {
        if (selectedTab == 1 && cleanRegisterUsername.length >= 3) {
            val (available, reason) = viewModel.checkUsernameAvailability(cleanRegisterUsername)
            isUsernameAvailable = available
            usernameFeedback = reason
        } else {
            isUsernameAvailable = null
            usernameFeedback = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Emblem / Monogram
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(TealPrimary, TealAccent, NexusViolet)
                        )
                    )
                    .padding(3.dp),
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
                        fontSize = 32.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CHATPRO",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Mensajería Rápida y Segura • Almacenamiento Local",
                fontSize = 12.sp,
                color = TealPrimary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Card Container with Modern Tab Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Segmented Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = TealPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = TealPrimary,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Text(
                                    "Iniciar Sesión",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (selectedTab == 0) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Text(
                                    "Crear Cuenta",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (selectedTab == 1) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Feedback Messages
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ErrorRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = successMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = OnlineGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OnlineGreen.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnlineGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage ?: "",
                                    color = OnlineGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // --- TAB 0: INICIAR SESIÓN ---
                    if (selectedTab == 0) {
                        // Username Field
                        OutlinedTextField(
                            value = loginUsername,
                            onValueChange = {
                                loginUsername = it.replace(" ", "")
                                errorMessage = null
                            },
                            label = { Text("Usuario (@usuario)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = TealPrimary
                                )
                            },
                            placeholder = { Text("@usuario") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                capitalization = KeyboardCapitalization.None
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = {
                                loginPassword = it
                                errorMessage = null
                            },
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TealPrimary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                    Icon(
                                        imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostrar u ocultar contraseña",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                val clean = loginUsername.trim().removePrefix("@")
                                if (clean.isBlank()) {
                                    errorMessage = "Por favor ingresa tu nombre de usuario."
                                    return@Button
                                }
                                isSubmitting = true
                                errorMessage = null
                                viewModel.loginUser(clean, loginPassword) { success, msg ->
                                    isSubmitting = false
                                    if (success) {
                                        successMessage = msg
                                        onSetupComplete()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                disabledContainerColor = TealPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Iniciar Sesión",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Storage Explanatory Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Los mensajes y archivos se almacenan de forma local y segura en la base de datos de tu dispositivo.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // --- TAB 1: CREAR CUENTA ---
                    if (selectedTab == 1) {
                        // Avatar Selector
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(listOf(TealPrimary, NexusViolet)),
                                    shape = CircleShape
                                )
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUriString.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(avatarUriString)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Elegir foto",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Text(
                                            text = "Añadir foto",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Display Name
                        OutlinedTextField(
                            value = registerDisplayName,
                            onValueChange = {
                                registerDisplayName = it
                                errorMessage = null
                            },
                            label = { Text("Nombre y Apellidos") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TealPrimary
                                )
                            },
                            placeholder = { Text("Ej: Carlos Pérez") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Username Handle (@...)
                        OutlinedTextField(
                            value = registerUsername,
                            onValueChange = {
                                registerUsername = it
                                errorMessage = null
                            },
                            label = { Text("Usuario (@usuario)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = TealPrimary
                                )
                            },
                            trailingIcon = {
                                when (isUsernameAvailable) {
                                    true -> Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Disponible",
                                        tint = OnlineGreen
                                    )
                                    false -> Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "No disponible",
                                        tint = ErrorRed
                                    )
                                    null -> null
                                }
                            },
                            placeholder = { Text("carlos_pro") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_username_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                capitalization = KeyboardCapitalization.None
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isUsernameAvailable == false) ErrorRed else TealPrimary,
                                unfocusedBorderColor = if (isUsernameAvailable == false) ErrorRed else MaterialTheme.colorScheme.outline
                            )
                        )

                        if (isUsernameAvailable == false && !usernameFeedback.isNullOrBlank()) {
                            Text(
                                text = usernameFeedback ?: "Usuario no disponible",
                                color = ErrorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 3.dp)
                            )
                        } else if (isUsernameAvailable == true) {
                            Text(
                                text = "✓ Nombre de usuario disponible",
                                color = OnlineGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = registerPassword,
                            onValueChange = {
                                registerPassword = it
                                errorMessage = null
                            },
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TealPrimary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showRegisterPassword = !showRegisterPassword }) {
                                    Icon(
                                        imageVector = if (showRegisterPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (showRegisterPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bio Field (Optional)
                        OutlinedTextField(
                            value = registerBio,
                            onValueChange = { registerBio = it },
                            label = { Text("Biografía (opcional)") },
                            placeholder = { Text("Desarrollador / Entusiasta • ChatPro") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Register Button
                        Button(
                            onClick = {
                                if (cleanRegisterUsername.length < 3) {
                                    errorMessage = "El usuario debe tener al menos 3 caracteres."
                                    return@Button
                                }
                                if (isUsernameAvailable == false) {
                                    errorMessage = usernameFeedback ?: "El nombre de usuario @$cleanRegisterUsername ya está registrado. Elige otro."
                                    return@Button
                                }
                                val isReserved = cleanRegisterUsername.equals("eliel_21", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("eliel", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("admin", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("administrador", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("administrator", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("root", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("system", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("moderator", ignoreCase = true) ||
                                        cleanRegisterUsername.equals("soporte", ignoreCase = true)
                                if (isReserved) {
                                    errorMessage = "El nombre de usuario @$cleanRegisterUsername está reservado para el administrador. No se puede crear esta cuenta."
                                    return@Button
                                }
                                if (registerDisplayName.trim().isBlank()) {
                                    errorMessage = "Por favor ingresa tu nombre."
                                    return@Button
                                }
                                isSubmitting = true
                                errorMessage = null
                                viewModel.registerUser(
                                    username = cleanRegisterUsername,
                                    displayName = registerDisplayName.trim(),
                                    bio = registerBio.trim(),
                                    avatarUrl = avatarUriString,
                                    password = registerPassword
                                ) { success, msg ->
                                    isSubmitting = false
                                    if (success) {
                                        successMessage = msg
                                        onSetupComplete()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("register_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSubmitting && isUsernameAvailable == true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                disabledContainerColor = TealPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Crear Cuenta y Entrar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Plataforma protegida con almacenamiento local Room",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
