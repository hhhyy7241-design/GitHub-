package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.GroupMemberWithUser
import com.example.data.local.MessageEntity
import com.example.data.local.MoodleConfigEntity
import com.example.data.local.StatusEntity
import com.example.data.local.UserEntity
import com.example.data.moodle.UcfMoodleClient
import com.example.data.repository.ChatRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.AudioCompressor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

sealed class Screen {
    object Onboarding : Screen()
    object SetupProfile : Screen()
    object ChatList : Screen()
    data class ChatDetail(val chatId: String) : Screen()
    object DeviceBanned : Screen()
}

data class UploadProgressState(
    val isUploading: Boolean = false,
    val fileName: String = "",
    val bytesSent: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = ChatRepository(database, application.applicationContext)

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.ChatList)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Theme Mode Preference
    private val prefs = application.getSharedPreferences("chatpro_theme_prefs", android.content.Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name)
        } catch (e: Exception) {
            AppThemeMode.DARK
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // Active Chat Selection
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Upload Progress
    private val _uploadProgress = MutableStateFlow(UploadProgressState())
    val uploadProgress: StateFlow<UploadProgressState> = _uploadProgress.asStateFlow()

    // UI Snackbars / Alerts
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Search query in chat list
    val searchQuery = MutableStateFlow("")

    // Tab filter: 0 = Todos, 1 = Privados, 2 = Grupos
    val selectedTab = MutableStateFlow(0)

    // Current user
    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Moodle Config
    val moodleConfig: StateFlow<MoodleConfigEntity?> = repository.moodleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active Statuses (Photos, Videos, Text)
    val activeStatuses: StateFlow<List<StatusEntity>> = repository.getActiveStatusesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Searched Contacts matching query
    val searchedContacts: StateFlow<List<UserEntity>> = combine(
        repository.allUsersFlow,
        currentUser,
        searchQuery
    ) { users, curUser, query ->
        val cleanQuery = query.trim().removePrefix("@").lowercase()
        if (cleanQuery.isBlank()) {
            emptyList()
        } else {
            val curUsername = curUser?.username?.lowercase() ?: ""
            users.filter { user ->
                val uName = user.username.lowercase()
                val dName = user.displayName.lowercase()
                uName != curUsername && (uName.contains(cleanQuery) || dName.contains(cleanQuery))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered chats list
    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        repository.allChatsFlow,
        searchQuery,
        selectedTab
    ) { chats, query, tab ->
        val q = query.trim().lowercase()
        chats.filter { chat ->
            val matchQuery = q.isEmpty() ||
                    chat.title.lowercase().contains(q) ||
                    chat.lastMessageSnippet.lowercase().contains(q) ||
                    chat.description.lowercase().contains(q)

            val matchTab = when (tab) {
                1 -> chat.type == "DIRECT"
                2 -> chat.type == "GROUP"
                else -> true
            }
            matchQuery && matchTab
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Entity
    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    // Active Interlocutor (for direct chats)
    private val _activeInterlocutor = MutableStateFlow<UserEntity?>(null)
    val activeInterlocutor: StateFlow<UserEntity?> = _activeInterlocutor.asStateFlow()

    // Active Messages
    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages.asStateFlow()

    // Active Group Members
    private val _activeGroupMembers = MutableStateFlow<List<GroupMemberWithUser>>(emptyList())
    val activeGroupMembers: StateFlow<List<GroupMemberWithUser>> = _activeGroupMembers.asStateFlow()

    // Inspecting User Profile (Interactive Sheet/Dialog)
    private val _inspectingUser = MutableStateFlow<UserEntity?>(null)
    val inspectingUser: StateFlow<UserEntity?> = _inspectingUser.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()

            if (repository.isCurrentDeviceBanned()) {
                _currentScreen.value = Screen.DeviceBanned
                return@launch
            }

            val user = repository.currentUserFlow.firstOrNull()
            if (user == null || !user.isCurrentUser) {
                _currentScreen.value = Screen.Onboarding
            } else {
                _currentScreen.value = Screen.ChatList
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.ChatDetail) {
            _activeChatId.value = screen.chatId
            observeChat(screen.chatId)
        }
    }

    fun navigateBack() {
        val current = _currentScreen.value
        if (current is Screen.ChatDetail) {
            chatObservationJob?.cancel()
            chatObservationJob = null
            _currentScreen.value = Screen.ChatList
            _activeChatId.value = null
            _activeChat.value = null
            _activeInterlocutor.value = null
            _activeMessages.value = emptyList()
            _activeGroupMembers.value = emptyList()
        }
    }

    private var chatObservationJob: kotlinx.coroutines.Job? = null

    private fun observeChat(chatId: String) {
        chatObservationJob?.cancel()
        chatObservationJob = viewModelScope.launch {
            repository.markChatAsRead(chatId)

            launch {
                repository.getChatByIdFlow(chatId).collect {
                    _activeChat.value = it
                }
            }
            launch {
                repository.getMessagesFlow(chatId).collect {
                    _activeMessages.value = it
                }
            }
            launch {
                repository.getGroupMembersFlow(chatId).collect {
                    _activeGroupMembers.value = it
                }
            }
            launch {
                if (chatId.startsWith("dm___") || chatId.startsWith("dm_") || chatId.startsWith("direct_")) {
                    val participants = ChatRepository.parseDirectChatParticipants(chatId)
                    combine(repository.currentUserFlow, repository.allUsersFlow) { curUser, allUsers ->
                        val cleanCur = curUser?.username?.trim()?.removePrefix("@")?.lowercase() ?: ""
                        val otherUsername = participants.firstOrNull { it != cleanCur } ?: participants.firstOrNull() ?: ""
                        allUsers.firstOrNull { it.username.trim().removePrefix("@").equals(otherUsername, ignoreCase = true) }
                    }.collect {
                        _activeInterlocutor.value = it
                    }
                } else {
                    _activeInterlocutor.value = null
                }
            }
        }
    }

    fun inspectUser(username: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null) {
                _inspectingUser.value = user
            } else {
                _snackbarEvent.emit("Usuario @$username no encontrado")
            }
        }
    }

    fun inspectUserEntity(user: UserEntity) {
        _inspectingUser.value = user
    }

    fun closeUserInspection() {
        _inspectingUser.value = null
    }

    fun banUserAndDevice(username: String) {
        viewModelScope.launch {
            val res = repository.banUserAndDevice(username)
            if (res.isSuccess) {
                _snackbarEvent.emit("Usuario @$username y su dispositivo han sido bloqueados permanentemente.")
                closeUserInspection()
            } else {
                _snackbarEvent.emit("Error al banear: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun loginUser(
        username: String,
        pass: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        chatObservationJob?.cancel()
        chatObservationJob = null
        _activeChatId.value = null
        _activeChat.value = null
        _activeMessages.value = emptyList()
        _activeGroupMembers.value = emptyList()

        viewModelScope.launch {
            val res = repository.loginUser(username, pass)
            if (res.isSuccess) {
                _currentScreen.value = Screen.ChatList
                val u = res.getOrNull()!!
                val welcome = if (u.role == "OWNER") "¡Bienvenido, Administrador @${u.username}!" else "¡Bienvenido a ChatPro, @${u.username}!"
                _snackbarEvent.emit(welcome)
                onComplete(true, welcome)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al iniciar sesión"
                if (err.contains("bloqueado", ignoreCase = true)) {
                    _currentScreen.value = Screen.DeviceBanned
                }
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun registerUser(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        password: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        chatObservationJob?.cancel()
        chatObservationJob = null
        _activeChatId.value = null
        _activeChat.value = null
        _activeMessages.value = emptyList()
        _activeGroupMembers.value = emptyList()

        viewModelScope.launch {
            val res = repository.registerInitialUser(
                username = username,
                displayName = displayName,
                bio = bio,
                avatarUrl = avatarUrl,
                password = password
            )
            if (res.isSuccess) {
                _currentScreen.value = Screen.ChatList
                _snackbarEvent.emit("¡Bienvenido a ChatPro, @$username!")
                onComplete(true, "Cuenta creada con éxito")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al crear cuenta"
                if (err.contains("bloqueado", ignoreCase = true)) {
                    _currentScreen.value = Screen.DeviceBanned
                }
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun sendMessage(
        text: String,
        replyToId: String? = null,
        replyToSender: String? = null,
        replyToText: String? = null
    ) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = repository.sendMessage(
                chatId = chatId,
                text = text.trim(),
                replyToId = replyToId,
                replyToSender = replyToSender,
                replyToText = replyToText
            )
            if (res.isFailure) {
                _snackbarEvent.emit("Error al enviar: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun sendAttachment(
        chatId: String,
        uri: Uri,
        attachmentType: String,
        replyToId: String? = null,
        replyToSender: String? = null,
        replyToText: String? = null
    ) {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch {
            try {
                _uploadProgress.value = UploadProgressState(isUploading = true, fileName = "Cargando archivo...", bytesSent = 0, totalBytes = 1, percentage = 0)

                var fileName = "adjunto_${System.currentTimeMillis()}"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                if (fileSize > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                    val sizeMb = String.format("%.2f", fileSize / (1024.0 * 1024.0))
                    _snackbarEvent.emit("El archivo supera el límite de 4.0 MB ($sizeMb MB).")
                    _uploadProgress.value = UploadProgressState(isUploading = false)
                    return@launch
                }

                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: byteArrayOf()
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                _uploadProgress.value = UploadProgressState(isUploading = true, fileName = fileName, bytesSent = bytes.size.toLong(), totalBytes = bytes.size.toLong(), percentage = 50)

                val res = repository.sendMessage(
                    chatId = chatId,
                    text = "",
                    attachmentBytes = bytes,
                    attachmentName = fileName,
                    attachmentType = attachmentType,
                    mimeType = mimeType,
                    replyToId = replyToId,
                    replyToSender = replyToSender,
                    replyToText = replyToText,
                    onUploadProgress = { sent, total ->
                        val percent = if (total > 0) ((sent.toDouble() / total) * 100).toInt() else 0
                        _uploadProgress.value = UploadProgressState(isUploading = true, fileName = fileName, bytesSent = sent, totalBytes = total, percentage = percent)
                    }
                )

                _uploadProgress.value = UploadProgressState(isUploading = false)

                if (res.isSuccess) {
                    _snackbarEvent.emit("Archivo enviado")
                } else {
                    _snackbarEvent.emit("Error al enviar archivo: ${res.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uploadProgress.value = UploadProgressState(isUploading = false)
                _snackbarEvent.emit("Error al leer archivo: ${e.message}")
            }
        }
    }

    fun createGroup(
        title: String,
        description: String = "",
        avatarUrl: String = "",
        initialMembers: List<String> = emptyList()
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val res = repository.createGroup(title, description, avatarUrl, initialMembers)
            if (res.isSuccess) {
                val group = res.getOrNull()!!
                _snackbarEvent.emit("Grupo \"${group.title}\" creado con éxito")
                navigateTo(Screen.ChatDetail(group.id))
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun sendAudioMessage(chatId: String, audioFile: java.io.File) {
        viewModelScope.launch {
            try {
                if (!audioFile.exists() || audioFile.length() < 100) {
                    _snackbarEvent.emit("La nota de voz es demasiado corta.")
                    return@launch
                }
                
                val compResult = AudioCompressor.compressAudio(getApplication(), audioFile)
                val finalFile = compResult.file
                val bytes = finalFile.readBytes()

                val totalSecs = (compResult.durationMs / 1000).coerceAtLeast(1)
                val durationLabel = String.format(java.util.Locale.US, "%d:%02d", totalSecs / 60, totalSecs % 60)

                val res = repository.sendMessage(
                    chatId = chatId,
                    text = "",
                    attachmentBytes = bytes,
                    attachmentName = "Nota de voz ($durationLabel)",
                    mimeType = compResult.mimeType,
                    attachmentType = "AUDIO"
                )
                if (res.isFailure) {
                    _snackbarEvent.emit("Error al enviar audio: ${res.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _snackbarEvent.emit("Error procesando nota de voz: ${e.message}")
            }
        }
    }

    fun createDirectChat(username: String, onResult: ((Boolean, String) -> Unit)? = null) {
        if (username.isBlank()) return
        viewModelScope.launch {
            val res = repository.createDirectChat(username)
            if (res.isSuccess) {
                val chat = res.getOrNull()!!
                _snackbarEvent.emit("Chat con @${username.removePrefix("@")} iniciado")
                navigateTo(Screen.ChatDetail(chat.id))
                onResult?.invoke(true, chat.id)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Usuario no encontrado"
                _snackbarEvent.emit(err)
                onResult?.invoke(false, err)
            }
        }
    }

    fun addMemberToGroup(chatId: String, username: String) {
        viewModelScope.launch {
            val res = repository.addMemberToGroup(chatId, username)
            if (res.isSuccess) {
                _snackbarEvent.emit("@$username añadido al grupo")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun removeMemberFromGroup(chatId: String, username: String) {
        viewModelScope.launch {
            val res = repository.removeMemberFromGroup(chatId, username)
            if (res.isSuccess) {
                _snackbarEvent.emit("@$username fue expulsado del grupo")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateMemberRole(chatId: String, username: String, newRole: String) {
        viewModelScope.launch {
            val res = repository.updateMemberRole(chatId, username, newRole)
            if (res.isSuccess) {
                val roleName = when (newRole) {
                    "ADMIN" -> "Administrador"
                    "OWNER" -> "Propietario"
                    else -> "Miembro"
                }
                _snackbarEvent.emit("Rol de @$username cambiado a $roleName")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun openDirectChatWithInspectedUser() {
        val user = _inspectingUser.value ?: return
        closeUserInspection()
        createDirectChat(user.username)
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            repository.updateCurrentUser(displayName, bio, avatarUrl)
            _snackbarEvent.emit("Perfil actualizado")
        }
    }

    fun testMoodleLogin(username: String, pass: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.testServerLogin(username, pass)
            if (res.isSuccess) {
                val msg = res.getOrNull() ?: "Conectado"
                _snackbarEvent.emit(msg)
                onComplete(true, msg)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Fallo de conexión"
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun saveMoodleConfig(config: MoodleConfigEntity) {
        viewModelScope.launch {
            repository.saveMoodleConfig(config)
            _snackbarEvent.emit("Configuración de servidor guardada")
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val res = repository.deleteMessage(messageId)
            if (res.isSuccess) {
                _snackbarEvent.emit("Mensaje eliminado")
            } else {
                _snackbarEvent.emit("Error al eliminar mensaje: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearChat(chatId: String) {
        viewModelScope.launch {
            val res = repository.clearChatHistory(chatId)
            if (res.isSuccess) {
                _snackbarEvent.emit("Historial del chat vaciado")
            } else {
                _snackbarEvent.emit("Error al vaciar chat: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // --- Stories / Statuses (Text, Photos, Videos) ---

    fun publishStatusText(
        text: String,
        backgroundColorHex: String = "#0D9488",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.publishStatus(text = text, backgroundColorHex = backgroundColorHex)
            if (res.isSuccess) {
                _snackbarEvent.emit("¡Estado publicado por 24 horas!")
                onComplete(true)
            } else {
                _snackbarEvent.emit("Error al publicar estado: ${res.exceptionOrNull()?.message}")
                onComplete(false)
            }
        }
    }

    fun publishMediaStatus(
        text: String,
        uri: Uri,
        isVideo: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch {
            try {
                var fileName = if (isVideo) "video_${System.currentTimeMillis()}.mp4" else "photo_${System.currentTimeMillis()}.jpg"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: byteArrayOf()

                val res = repository.publishStatus(
                    text = text,
                    mediaBytes = bytes,
                    mediaName = fileName,
                    mediaType = if (isVideo) "VIDEO" else "IMAGE"
                )

                if (res.isSuccess) {
                    _snackbarEvent.emit("¡${if (isVideo) "Video" else "Foto"} de estado publicado!")
                    onComplete(true)
                } else {
                    _snackbarEvent.emit("Error al publicar estado: ${res.exceptionOrNull()?.message}")
                    onComplete(false)
                }
            } catch (e: Exception) {
                _snackbarEvent.emit("Error al cargar archivo: ${e.message}")
                onComplete(false)
            }
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatus(statusId)
            _snackbarEvent.emit("Estado eliminado")
        }
    }

    fun sendAttachment(
        uri: Uri,
        isImage: Boolean,
        replyToId: String? = null,
        replyToSender: String? = null,
        replyToText: String? = null
    ) {
        val chatId = _activeChatId.value ?: return
        sendAttachment(
            chatId = chatId,
            uri = uri,
            attachmentType = if (isImage) "IMAGE" else "FILE",
            replyToId = replyToId,
            replyToSender = replyToSender,
            replyToText = replyToText
        )
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            val res = repository.editMessage(messageId, newText)
            if (res.isSuccess) {
                _snackbarEvent.emit("Mensaje editado")
            } else {
                _snackbarEvent.emit("Error al editar: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteAttachment(messageId: String) {
        viewModelScope.launch {
            val res = repository.deleteAttachment(messageId)
            if (res.isSuccess) {
                _snackbarEvent.emit("Archivo adjunto eliminado")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    suspend fun getAllDirectoryUsers(): List<UserEntity> = repository.getAllDirectoryUsers()

    suspend fun checkUserExists(username: String): UserEntity? = repository.getUserByUsername(username)

    suspend fun getAllLocalAccounts(): List<UserEntity> = repository.getAllLocalAccounts()

    suspend fun checkUsernameAvailability(username: String): Pair<Boolean, String> =
        repository.checkUsernameAvailability(username, currentUser.value?.username)

    fun switchAccount(username: String) {
        chatObservationJob?.cancel()
        chatObservationJob = null
        _activeChatId.value = null
        _activeChat.value = null
        _activeMessages.value = emptyList()
        _activeGroupMembers.value = emptyList()
        viewModelScope.launch {
            val res = repository.switchAccount(username)
            if (res.isSuccess) {
                val u = res.getOrNull()!!
                _currentScreen.value = Screen.ChatList
                _snackbarEvent.emit("Sesión cambiada a @${u.username}")
            } else {
                _snackbarEvent.emit("Error al cambiar de cuenta: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteSpecificAccount(username: String) {
        viewModelScope.launch {
            val res = repository.deleteAccount(username)
            if (res.isSuccess) {
                _snackbarEvent.emit("Cuenta @$username eliminada de este dispositivo.")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteAccount() {
        chatObservationJob?.cancel()
        chatObservationJob = null
        _activeChatId.value = null
        _activeChat.value = null
        _activeMessages.value = emptyList()
        _activeGroupMembers.value = emptyList()
        viewModelScope.launch {
            val res = repository.deleteAccount()
            if (res.isSuccess) {
                _currentScreen.value = Screen.SetupProfile
                _snackbarEvent.emit("Tu cuenta ha sido eliminada permanentemente.")
            } else {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun logout() {
        chatObservationJob?.cancel()
        chatObservationJob = null
        _activeChatId.value = null
        _activeChat.value = null
        _activeMessages.value = emptyList()
        _activeGroupMembers.value = emptyList()
        viewModelScope.launch {
            repository.logout()
            _currentScreen.value = Screen.SetupProfile
            _snackbarEvent.emit("Has cerrado sesión.")
        }
    }
}
