package com.example.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.BannedDeviceEntity
import com.example.data.local.ChatEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.GroupMemberWithUser
import com.example.data.local.MessageEntity
import com.example.data.local.MoodleConfigEntity
import com.example.data.local.StatusEntity
import com.example.data.local.UserEntity
import com.example.data.moodle.ChatSyncBundleJson
import com.example.data.moodle.MessagePayloadJson
import com.example.data.moodle.UcfMoodleClient
import com.example.data.notification.NotificationHelper
import com.example.security.SecurityShield
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ChatRepository(
    private val database: AppDatabase,
    private val context: Context,
    val moodleClient: UcfMoodleClient = UcfMoodleClient()
) {
    private val userDao = database.userDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val moodleConfigDao = database.moodleConfigDao()
    private val groupMemberDao = database.groupMemberDao()
    private val statusDao = database.statusDao()
    private val bannedDeviceDao = database.bannedDeviceDao()

    companion object {
        const val GENERAL_GROUP_ID = "group_chatpro_general"
        val ADMIN_USERNAME: String get() = SecurityShield.SECURE_ADMIN_USER
        val ADMIN_PASSWORD: String get() = SecurityShield.SECURE_ADMIN_PASS

        fun parseDirectChatParticipants(chatId: String): List<String> {
            val raw = chatId.removePrefix("dm___").removePrefix("dm_").removePrefix("direct_")
            return if (chatId.contains("___")) {
                raw.split("___").map { it.trim().removePrefix("@").lowercase() }
            } else {
                raw.split("_").map { it.trim().removePrefix("@").lowercase() }
            }
        }

        fun makeDirectChatId(u1: String, u2: String): String {
            val clean1 = u1.trim().removePrefix("@").lowercase()
            val clean2 = u2.trim().removePrefix("@").lowercase()
            return if (clean1 <= clean2) "dm___${clean1}___${clean2}" else "dm___${clean2}___${clean1}"
        }
    }

    fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_unknown"
        } catch (e: Exception) {
            "device_unknown"
        }
    }

    suspend fun isCurrentDeviceBanned(): Boolean = withContext(Dispatchers.IO) {
        val devId = getDeviceId()
        val banned = bannedDeviceDao.getBannedDevice(devId)
        if (banned != null) return@withContext true

        val cur = userDao.getCurrentUser()
        if (cur != null) {
            val bannedUser = bannedDeviceDao.getBannedByUsername(cur.username)
            if (bannedUser != null) return@withContext true
        }
        false
    }

    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val allUsersFlow: Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    val allChatsFlow: Flow<List<ChatEntity>> = combine(
        currentUserFlow,
        chatDao.getAllChatsFlow(),
        userDao.getAllUsersFlow(),
        groupMemberDao.getAllGroupMembersFlow()
    ) { currentUser, allChats, allUsers, allMembers ->
        if (currentUser == null) {
            emptyList()
        } else {
            val cleanCurrent = currentUser.username.trim().removePrefix("@").lowercase()
            val userMap = allUsers.associateBy { it.username.trim().removePrefix("@").lowercase() }
            val memberChatIds = allMembers
                .filter { it.username.trim().removePrefix("@").equals(cleanCurrent, ignoreCase = true) }
                .map { it.chatId }
                .toSet()

            allChats.mapNotNull { chat ->
                if (chat.type == "DIRECT") {
                    val participants = parseDirectChatParticipants(chat.id)
                    val isParticipant = participants.contains(cleanCurrent) ||
                            chat.id.lowercase().contains(cleanCurrent)

                    if (!isParticipant) {
                        null
                    } else {
                        val otherUsername = participants.firstOrNull { it != cleanCurrent }
                            ?: participants.firstOrNull() ?: ""
                        val otherUser = userMap[otherUsername] ?: userDao.getUserByUsername(otherUsername)
                        if (otherUser != null) {
                            chat.copy(
                                title = otherUser.displayName,
                                avatarUrl = otherUser.avatarUrl,
                                description = "@${otherUser.username}"
                            )
                        } else {
                            chat
                        }
                    }
                } else {
                    val isMember = chat.id == GENERAL_GROUP_ID || memberChatIds.contains(chat.id)
                    if (isMember) chat else null
                }
            }
        }
    }

    val moodleConfigFlow: Flow<MoodleConfigEntity?> = moodleConfigDao.getConfigFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getMessagesFlow(chatId: String): Flow<List<MessageEntity>> =
        userDao.getCurrentUserFlow().flatMapLatest { currentUser ->
            val curUser = currentUser?.username ?: ""
            messageDao.getMessagesForChatFlow(chatId).map { messages ->
                messages.map { msg ->
                    msg.copy(isOutgoing = msg.senderId.equals(curUser, ignoreCase = true))
                }
            }
        }

    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?> =
        combine(
            currentUserFlow,
            chatDao.getChatByIdFlow(chatId),
            userDao.getAllUsersFlow()
        ) { currentUser, chat, allUsers ->
            if (chat == null || currentUser == null) {
                chat
            } else if (chat.type == "DIRECT") {
                val cleanCurrent = currentUser.username.trim().removePrefix("@").lowercase()
                val participants = parseDirectChatParticipants(chat.id)
                val otherUsername = participants.firstOrNull { it != cleanCurrent } ?: participants.firstOrNull() ?: ""
                val userMap = allUsers.associateBy { it.username.trim().removePrefix("@").lowercase() }
                val otherUser = userMap[otherUsername] ?: userDao.getUserByUsername(otherUsername)
                if (otherUser != null) {
                    chat.copy(
                        title = otherUser.displayName,
                        avatarUrl = otherUser.avatarUrl,
                        description = "@${otherUser.username}"
                    )
                } else {
                    chat
                }
            } else {
                chat
            }
        }

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        val existingConfig = moodleConfigDao.getConfig()
        val defaultConfig = MoodleConfigEntity(
            id = 1,
            host = SecurityShield.SECURE_HOST,
            username = SecurityShield.SECURE_DEFAULT_USER,
            password = SecurityShield.SECURE_DEFAULT_PASS,
            repoId = 4,
            uploadType = "evidence",
            maxChunkBytes = 4 * 1024 * 1024L
        )

        if (existingConfig == null) {
            moodleConfigDao.saveConfig(defaultConfig)
        }
        val activeConfig = existingConfig ?: defaultConfig
        moodleClient.configure(activeConfig.host, activeConfig.repoId)

        // Initialize general community group
        val generalGroup = chatDao.getChatById(GENERAL_GROUP_ID)
        if (generalGroup == null) {
            chatDao.insertOrUpdate(
                ChatEntity(
                    id = GENERAL_GROUP_ID,
                    title = "Comunidad General ChatPro",
                    type = "GROUP",
                    avatarUrl = "",
                    description = "Grupo oficial de la comunidad universitaria y nacional",
                    unreadCount = 0,
                    lastMessageSnippet = "¡Bienvenidos a la red de ChatPro!",
                    lastMessageTime = System.currentTimeMillis(),
                    pinned = true
                )
            )
        }

        // Initialize Admin / Owner User
        val adminUser = userDao.getUserByUsername(ADMIN_USERNAME)
        val devId = getDeviceId()
        if (adminUser == null) {
            userDao.insertOrUpdate(
                UserEntity(
                    username = ADMIN_USERNAME,
                    displayName = "Eliel (Administrador)",
                    bio = "👑 Creador y Administrador de ChatPro",
                    avatarUrl = "",
                    isCurrentUser = false,
                    lastSeen = System.currentTimeMillis(),
                    password = ADMIN_PASSWORD,
                    role = "OWNER",
                    deviceId = devId
                )
            )
        }

        syncRemoteEvidences()
    }

    suspend fun checkUsernameAvailability(username: String, currentUsername: String? = null): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            if (clean.length < 3) return@withContext Pair(false, "Mínimo 3 caracteres")
            val currentClean = currentUsername?.trim()?.removePrefix("@")
            if (clean.equals(currentClean, ignoreCase = true)) return@withContext Pair(true, "Tu nombre actual")

            val isReserved = clean.equals(ADMIN_USERNAME, ignoreCase = true) ||
                    clean.equals("eliel_21", ignoreCase = true) ||
                    clean.equals("admin", ignoreCase = true)
            if (isReserved) return@withContext Pair(false, "Nombre reservado para el Administrador")

            val existing = userDao.getUserByUsername(clean)
            if (existing != null) return@withContext Pair(false, "Nombre de usuario ya en uso")

            Pair(true, "Disponible ✓")
        }

    suspend fun loginUser(username: String, pass: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (isCurrentDeviceBanned()) {
            return@withContext Result.failure(IllegalStateException("Este dispositivo ha sido bloqueado permanentemente por el Administrador."))
        }

        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa tu usuario"))
        }

        val isOwnerLogin = clean.equals("Eliel_21", ignoreCase = true) ||
                clean.equals("eliel", ignoreCase = true) ||
                clean.equals("admin", ignoreCase = true)

        val devId = getDeviceId()
        val existing = userDao.getUserByUsername(clean)
        val userToLogin = if (existing != null) {
            existing.copy(
                isCurrentUser = true,
                lastSeen = System.currentTimeMillis(),
                deviceId = devId
            )
        } else {
            UserEntity(
                username = clean,
                displayName = if (isOwnerLogin) "Eliel (Administrador)" else clean,
                bio = if (isOwnerLogin) "👑 Administrador de ChatPro" else "Usuario de ChatPro",
                avatarUrl = "",
                isCurrentUser = true,
                lastSeen = System.currentTimeMillis(),
                password = pass,
                role = if (isOwnerLogin) "OWNER" else "MEMBER",
                deviceId = devId
            )
        }

        userDao.clearCurrentUserFlag()
        userDao.insertOrUpdate(userToLogin)

        groupMemberDao.insertOrUpdate(
            GroupMemberEntity(
                chatId = GENERAL_GROUP_ID,
                username = clean,
                role = if (userToLogin.role == "OWNER") "OWNER" else "MEMBER"
            )
        )

        syncRemoteEvidences()
        Result.success(userToLogin)
    }

    suspend fun registerInitialUser(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        password: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (isCurrentDeviceBanned()) {
            return@withContext Result.failure(IllegalStateException("Este dispositivo ha sido bloqueado permanentemente por el Administrador."))
        }

        val cleanUsername = username.trim().removePrefix("@")
        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de usuario no puede estar vacío."))
        }

        val isOwner = cleanUsername.equals("Eliel_21", ignoreCase = true) ||
                cleanUsername.equals("admin", ignoreCase = true)

        val existing = userDao.getUserByUsername(cleanUsername)
        if (existing != null && !isOwner) {
            return@withContext Result.failure(IllegalStateException("El nombre de usuario @$cleanUsername ya está registrado. Por favor elige otro nombre o inicia sesión."))
        }

        val devId = getDeviceId()
        val newUser = UserEntity(
            username = cleanUsername,
            displayName = displayName.ifBlank { cleanUsername },
            bio = bio,
            avatarUrl = avatarUrl,
            isCurrentUser = true,
            lastSeen = System.currentTimeMillis(),
            password = password,
            role = if (isOwner) "OWNER" else "MEMBER",
            deviceId = devId
        )

        userDao.clearCurrentUserFlag()
        userDao.insertOrUpdate(newUser)

        try {
            val userJson = JSONObject().apply {
                put("username", newUser.username)
                put("displayName", newUser.displayName)
                put("bio", newUser.bio)
                put("avatarUrl", newUser.avatarUrl)
                put("role", newUser.role)
                put("deviceId", devId)
                put("registeredAt", System.currentTimeMillis())
                put("lastSeen", System.currentTimeMillis())
            }
            moodleClient.uploadToEvidence(
                evidenceName = "[ChatPro_User] ${newUser.username}",
                fileName = "user_${newUser.username}.json",
                fileBytes = userJson.toString(2).toByteArray(Charsets.UTF_8),
                mimeType = "application/json"
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error uploading user to Moodle: ${e.message}")
        }

        groupMemberDao.insertOrUpdate(
            GroupMemberEntity(
                chatId = GENERAL_GROUP_ID,
                username = cleanUsername,
                role = if (isOwner) "OWNER" else "MEMBER"
            )
        )

        Result.success(newUser)
    }

    suspend fun banUserAndDevice(
        targetUsername: String,
        reason: String = "Expulsado y bloqueado permanentemente por el Administrador"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = userDao.getCurrentUser()
        val isOwner = currentUser?.role == "OWNER" ||
                currentUser?.username.equals("Eliel_21", ignoreCase = true) ||
                currentUser?.username.equals("admin", ignoreCase = true)
        if (!isOwner) {
            return@withContext Result.failure(IllegalAccessException("Solo el Administrador puede expulsar y bloquear dispositivos."))
        }

        val clean = targetUsername.trim().removePrefix("@")
        val targetUser = userDao.getUserByUsername(clean)
        val devId = targetUser?.deviceId?.takeIf { it.isNotBlank() && it != "device_unknown" } ?: "ban_${clean}"

        val bannedEntity = BannedDeviceEntity(
            deviceId = devId,
            username = clean,
            reason = reason,
            bannedAt = System.currentTimeMillis()
        )
        bannedDeviceDao.insertOrUpdate(bannedEntity)

        try {
            val banJson = JSONObject().apply {
                put("deviceId", devId)
                put("username", clean)
                put("reason", reason)
                put("bannedAt", System.currentTimeMillis())
            }
            moodleClient.uploadToEvidence(
                evidenceName = "[ChatPro_Ban] $clean",
                fileName = "ban_${clean}.json",
                fileBytes = banJson.toString(2).toByteArray(Charsets.UTF_8),
                mimeType = "application/json"
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error uploading ban: ${e.message}")
        }

        userDao.deleteByUsername(clean)
        Result.success(Unit)
    }

    suspend fun createDirectChat(targetUsername: String): Result<ChatEntity> =
        withContext(Dispatchers.IO) {
            val clean = targetUsername.trim().removePrefix("@")
            if (clean.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Ingresa un nombre de usuario."))
            }
            val currentUser = userDao.getCurrentUser()
                ?: return@withContext Result.failure(IllegalStateException("No hay sesión de usuario activa."))
            if (clean.equals(currentUser.username, ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("No puedes iniciar una conversación contigo mismo."))
            }
            val targetUser = userDao.getUserByUsername(clean)
                ?: UserEntity(
                    username = clean,
                    displayName = clean,
                    bio = "",
                    avatarUrl = "",
                    isCurrentUser = false
                ).also { userDao.insertOrUpdate(it) }

            val chatId = makeDirectChatId(currentUser.username, clean)
            val existingChat = chatDao.getChatById(chatId)
            if (existingChat != null) {
                return@withContext Result.success(existingChat)
            }
            val chat = ChatEntity(
                id = chatId,
                title = targetUser.displayName,
                type = "DIRECT",
                avatarUrl = targetUser.avatarUrl,
                description = "@${targetUser.username}",
                unreadCount = 0,
                lastMessageSnippet = "Conversación iniciada con @$clean",
                lastMessageTime = System.currentTimeMillis(),
                pinned = false
            )
            chatDao.insertOrUpdate(chat)
            Result.success(chat)
        }

    suspend fun createGroup(
        title: String,
        description: String = "",
        avatarUrl: String = "",
        initialMemberUsernames: List<String> = emptyList()
    ): Result<ChatEntity> = withContext(Dispatchers.IO) {
        val currentUser = userDao.getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("Usuario no autenticado"))

        val isOwner = currentUser.role == "OWNER" ||
                currentUser.username.equals("eliel_21", ignoreCase = true) ||
                currentUser.username.equals(ADMIN_USERNAME, ignoreCase = true)
        if (!isOwner) {
            return@withContext Result.failure(IllegalAccessException("Solo el administrador puede crear grupos."))
        }

        val groupId = "group_${UUID.randomUUID().toString().take(8)}"
        val group = ChatEntity(
            id = groupId,
            title = title.trim(),
            type = "GROUP",
            avatarUrl = avatarUrl.trim(),
            description = description.trim(),
            unreadCount = 0,
            lastMessageSnippet = "Grupo creado por @${currentUser.username}",
            lastMessageTime = System.currentTimeMillis(),
            pinned = false
        )
        chatDao.insertOrUpdate(group)

        groupMemberDao.insertOrUpdate(
            GroupMemberEntity(
                chatId = groupId,
                username = currentUser.username,
                role = "OWNER"
            )
        )

        initialMemberUsernames.forEach { rawMember ->
            val cleanMember = rawMember.trim().removePrefix("@")
            if (cleanMember.isNotBlank() && !cleanMember.equals(currentUser.username, ignoreCase = true)) {
                val memberUser = userDao.getUserByUsername(cleanMember)
                if (memberUser != null) {
                    groupMemberDao.insertOrUpdate(
                        GroupMemberEntity(
                            chatId = groupId,
                            username = cleanMember,
                            role = "MEMBER"
                        )
                    )
                }
            }
        }

        Result.success(group)
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        attachmentBytes: ByteArray? = null,
        attachmentName: String? = null,
        attachmentType: String? = null,
        mimeType: String = "application/octet-stream",
        replyToId: String? = null,
        replyToSender: String? = null,
        replyToText: String? = null,
        onUploadProgress: ((Long, Long) -> Unit)? = null
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        val currentUser = userDao.getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("Usuario no autenticado"))

        val messageId = "msg_${UUID.randomUUID()}"
        var finalAttachmentUrl: String? = null
        var uploadedEvidenceId: String? = null
        val attachmentSize = attachmentBytes?.size?.toLong() ?: 0L

        if (attachmentBytes != null && !attachmentName.isNullOrEmpty()) {
            if (attachmentSize > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                val sizeMb = String.format("%.2f", attachmentSize / (1024.0 * 1024.0))
                return@withContext Result.failure(
                    IllegalArgumentException("El archivo supera el límite permitido de 4.0 MB ($sizeMb MB).")
                )
            }

            try {
                val attachmentsDir = File(context.filesDir, "attachments")
                if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
                val safeFileName = "${System.currentTimeMillis()}_${attachmentName.replace(" ", "_")}"
                val localFile = File(attachmentsDir, safeFileName)
                localFile.writeBytes(attachmentBytes)
                finalAttachmentUrl = localFile.absolutePath
            } catch (ignored: Exception) {}

            try {
                val evName = "[ChatPro_File] ${currentUser.username}_${messageId.take(10)}"
                val uploadResult = moodleClient.uploadToEvidence(
                    evidenceName = evName,
                    fileName = attachmentName,
                    fileBytes = attachmentBytes,
                    mimeType = mimeType
                )
                if (uploadResult.isSuccess) {
                    val evData = uploadResult.getOrNull()
                    uploadedEvidenceId = evData?.evidenceId
                    if (!evData?.url.isNullOrBlank()) {
                        finalAttachmentUrl = evData?.url
                    }
                }
            } catch (ignored: Exception) {}
        }

        val message = MessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = currentUser.username,
            senderName = currentUser.displayName,
            senderAvatar = currentUser.avatarUrl,
            timestamp = System.currentTimeMillis(),
            text = text,
            attachmentUrl = finalAttachmentUrl,
            attachmentName = attachmentName,
            attachmentType = attachmentType,
            attachmentSize = attachmentSize,
            isOutgoing = true,
            status = "SENT",
            evidenceId = uploadedEvidenceId,
            isEdited = false,
            replyToId = replyToId,
            replyToSender = replyToSender,
            replyToText = replyToText
        )
        messageDao.insertOrUpdate(message)

        val snippet = when {
            attachmentType == "AUDIO" -> "🎤 Nota de voz"
            attachmentType == "VIDEO" -> "🎥 Video"
            attachmentType == "IMAGE" -> "📷 Imagen"
            attachmentType == "DOCUMENT" -> "📄 Archivo (${attachmentName ?: "adjunto"})"
            text.isNotBlank() -> text
            else -> "Adjunto"
        }
        chatDao.updateLastMessage(chatId, snippet, message.timestamp, message.senderId)

        try {
            val isDirect = chatId.startsWith("dm___") || chatId.startsWith("dm_") || chatId.startsWith("direct_")
            val evidenceTitle = if (isDirect) {
                val participants = parseDirectChatParticipants(chatId)
                val cleanCur = currentUser.username.trim().removePrefix("@").lowercase()
                val other = participants.firstOrNull { it != cleanCur } ?: participants.firstOrNull() ?: ""
                "[ChatPro_DM] ${cleanCur}_to_${other}_${messageId.take(10)}"
            } else {
                "[ChatPro_Group] ${chatId}_${currentUser.username}_${messageId.take(10)}"
            }

            val msgPayload = JSONObject().apply {
                put("id", message.id)
                put("chatId", message.chatId)
                put("senderId", message.senderId)
                put("senderName", message.senderName)
                put("senderAvatar", message.senderAvatar)
                put("timestamp", message.timestamp)
                put("text", message.text)
                put("attachmentUrl", message.attachmentUrl)
                put("attachmentName", message.attachmentName)
                put("attachmentType", message.attachmentType)
                put("attachmentSize", message.attachmentSize)
                put("replyToId", message.replyToId)
                put("replyToSender", message.replyToSender)
                put("replyToText", message.replyToText)
            }

            val uploadRes = moodleClient.uploadToEvidence(
                evidenceName = evidenceTitle,
                fileName = "msg_${messageId.take(10)}.json",
                fileBytes = msgPayload.toString(2).toByteArray(Charsets.UTF_8),
                mimeType = "application/json"
            )

            if (uploadRes.isSuccess) {
                messageDao.updateMessageStatus(messageId, "DELIVERED")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error uploading message payload to Moodle: ${e.message}")
        }

        Result.success(message)
    }

    suspend fun editMessage(messageId: String, newText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val msg = messageDao.getMessageById(messageId) ?: return@withContext Result.failure(IllegalArgumentException("Mensaje no encontrado"))
        val updated = msg.copy(text = newText, isEdited = true)
        messageDao.insertOrUpdate(updated)
        Result.success(Unit)
    }

    suspend fun deleteAttachment(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null && !msg.evidenceId.isNullOrBlank()) {
            try {
                moodleClient.deleteEvidence(msg.evidenceId)
            } catch (ignored: Exception) {}
        }
        messageDao.clearAttachment(messageId)
        Result.success(Unit)
    }

    suspend fun markChatAsRead(chatId: String): Unit = withContext(Dispatchers.IO) {
        messageDao.markMessagesAsRead(chatId)
        chatDao.updateUnreadCount(chatId, 0)
    }

    suspend fun syncRemoteEvidences() = withContext(Dispatchers.IO) {
        val config = moodleConfigDao.getConfig() ?: return@withContext
        val currentUser = userDao.getCurrentUser() ?: return@withContext
        val cleanCurrent = currentUser.username.trim().removePrefix("@").lowercase()

        try {
            moodleClient.configure(config.host, config.repoId)
            if (config.username.isNotBlank() && config.password.isNotBlank()) {
                moodleClient.login(config.username, config.password)
            }

            val evidencesRes = moodleClient.getEvidences()
            if (evidencesRes.isSuccess) {
                val evidences = evidencesRes.getOrNull() ?: emptyList()

                for (ev in evidences) {
                    val name = ev.name

                    if (name.startsWith("[ChatPro_Ban]")) {
                        val bannedUser = name.removePrefix("[ChatPro_Ban] ").trim().lowercase()
                        if (bannedUser == cleanCurrent) {
                            val myDevId = getDeviceId()
                            bannedDeviceDao.insertOrUpdate(
                                BannedDeviceEntity(
                                    deviceId = myDevId,
                                    username = cleanCurrent,
                                    reason = "Expulsado y bloqueado por el Administrador",
                                    bannedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    if (name.startsWith("[ChatPro_DM] ") && name.contains("_to_${cleanCurrent}_")) {
                        val senderPart = name.substringAfter("[ChatPro_DM] ").substringBefore("_to_").trim()
                        val chatId = makeDirectChatId(cleanCurrent, senderPart)

                        var chat = chatDao.getChatById(chatId)
                        if (chat == null) {
                            val senderUser = userDao.getUserByUsername(senderPart) ?: UserEntity(
                                username = senderPart,
                                displayName = senderPart,
                                bio = "",
                                avatarUrl = "",
                                isCurrentUser = false
                            ).also { userDao.insertOrUpdate(it) }

                            chat = ChatEntity(
                                id = chatId,
                                title = senderUser.displayName,
                                type = "DIRECT",
                                avatarUrl = senderUser.avatarUrl,
                                description = "@${senderUser.username}",
                                unreadCount = 0,
                                lastMessageSnippet = "Nuevo mensaje recibido",
                                lastMessageTime = System.currentTimeMillis(),
                                pinned = false
                            )
                            chatDao.insertOrUpdate(chat)
                        }

                        val existing = messageDao.getMessageById(ev.id)
                        val fileUrl = ev.files.firstOrNull() ?: ""
                        if (existing == null && fileUrl.isNotBlank()) {
                            val jsonRes = moodleClient.fetchJsonFromUrl(fileUrl)
                            if (jsonRes.isSuccess) {
                                val jsonStr = jsonRes.getOrNull() ?: ""
                                try {
                                    val obj = JSONObject(jsonStr)
                                    val newMsg = MessageEntity(
                                        id = obj.optString("id", ev.id),
                                        chatId = chatId,
                                        senderId = obj.optString("senderId", senderPart),
                                        senderName = obj.optString("senderName", senderPart),
                                        senderAvatar = obj.optString("senderAvatar", ""),
                                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                        text = obj.optString("text", ""),
                                        attachmentUrl = if (obj.has("attachmentUrl") && !obj.isNull("attachmentUrl")) obj.getString("attachmentUrl") else null,
                                        attachmentName = if (obj.has("attachmentName") && !obj.isNull("attachmentName")) obj.getString("attachmentName") else null,
                                        attachmentType = if (obj.has("attachmentType") && !obj.isNull("attachmentType")) obj.getString("attachmentType") else null,
                                        attachmentSize = obj.optLong("attachmentSize", 0L),
                                        isOutgoing = false,
                                        status = "DELIVERED",
                                        evidenceId = ev.id,
                                        replyToId = if (obj.has("replyToId") && !obj.isNull("replyToId")) obj.getString("replyToId") else null,
                                        replyToSender = if (obj.has("replyToSender") && !obj.isNull("replyToSender")) obj.getString("replyToSender") else null,
                                        replyToText = if (obj.has("replyToText") && !obj.isNull("replyToText")) obj.getString("replyToText") else null
                                    )
                                    messageDao.insertOrUpdate(newMsg)
                                    val snip = when {
                                        newMsg.attachmentType == "AUDIO" -> "🎤 Nota de voz"
                                        newMsg.attachmentType == "VIDEO" -> "🎥 Video"
                                        newMsg.attachmentType == "IMAGE" -> "📷 Imagen"
                                        newMsg.text.isNotBlank() -> newMsg.text
                                        else -> "Archivo adjunto"
                                    }
                                    chatDao.updateLastMessage(chatId, snip, newMsg.timestamp, newMsg.senderId)
                                } catch (ignored: Exception) {}
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Sync evidences error: ${e.message}")
        }
    }

    fun getActiveStatusesFlow(): Flow<List<StatusEntity>> =
        statusDao.getActiveStatusesFlow(System.currentTimeMillis())

    suspend fun publishStatus(
        text: String,
        mediaBytes: ByteArray? = null,
        mediaName: String? = null,
        mediaType: String = "IMAGE",
        backgroundColorHex: String = "#0D9488"
    ): Result<StatusEntity> = withContext(Dispatchers.IO) {
        val currentUser = userDao.getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("Inicia sesión para publicar un estado."))

        val statusId = "status_${UUID.randomUUID().toString().take(8)}"
        var finalMediaUrl: String? = null
        var uploadedEvidenceId: String? = null

        if (mediaBytes != null && !mediaName.isNullOrEmpty()) {
            try {
                val statusesDir = File(context.filesDir, "statuses")
                if (!statusesDir.exists()) statusesDir.mkdirs()
                val safeName = "${System.currentTimeMillis()}_${mediaName.replace(" ", "_")}"
                val localFile = File(statusesDir, safeName)
                localFile.writeBytes(mediaBytes)
                finalMediaUrl = localFile.absolutePath

                val evName = "[ChatPro_Status] ${currentUser.username}_${statusId}"
                val mime = if (mediaType == "VIDEO") "video/mp4" else "image/jpeg"
                val upRes = moodleClient.uploadToEvidence(
                    evidenceName = evName,
                    fileName = mediaName,
                    fileBytes = mediaBytes,
                    mimeType = mime
                )
                if (upRes.isSuccess) {
                    val evData = upRes.getOrNull()
                    uploadedEvidenceId = evData?.evidenceId
                    if (!evData?.url.isNullOrBlank()) {
                        finalMediaUrl = evData?.url
                    }
                }
            } catch (ignored: Exception) {}
        }

        val newStatus = StatusEntity(
            id = statusId,
            authorUsername = currentUser.username,
            authorDisplayName = currentUser.displayName,
            authorAvatar = currentUser.avatarUrl,
            text = text.trim(),
            mediaUrl = finalMediaUrl,
            mediaType = mediaType,
            backgroundColorHex = backgroundColorHex,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
            evidenceId = uploadedEvidenceId
        )

        statusDao.insertOrUpdate(newStatus)
        Result.success(newStatus)
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val st = statusDao.getStatusById(statusId)
        if (st != null && !st.evidenceId.isNullOrBlank()) {
            try {
                moodleClient.deleteEvidence(st.evidenceId)
            } catch (ignored: Exception) {}
        }
        statusDao.deleteStatus(statusId)
        Result.success(Unit)
    }

    suspend fun getGroupMembersFlow(chatId: String): Flow<List<GroupMemberWithUser>> =
        groupMemberDao.getMembersForChatFlow(chatId).map { members ->
            members.map { member ->
                GroupMemberWithUser(
                    member = member,
                    user = userDao.getUserByUsername(member.username)
                )
            }
        }

    suspend fun addMemberToGroup(chatId: String, username: String, role: String = "MEMBER"): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            val user = userDao.getUserByUsername(clean)
                ?: return@withContext Result.failure(IllegalArgumentException("El usuario @$clean no existe."))
            groupMemberDao.insertOrUpdate(
                GroupMemberEntity(
                    chatId = chatId,
                    username = clean,
                    role = role
                )
            )
            Result.success(Unit)
        }

    suspend fun removeMemberFromGroup(chatId: String, username: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            val member = groupMemberDao.getMember(chatId, clean)
                ?: return@withContext Result.failure(IllegalArgumentException("Miembro no encontrado."))
            if (member.role == "OWNER") {
                return@withContext Result.failure(IllegalArgumentException("No se puede expulsar al creador del grupo."))
            }
            groupMemberDao.removeMember(chatId, clean)
            Result.success(Unit)
        }

    suspend fun deleteMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null && !msg.evidenceId.isNullOrBlank()) {
            try {
                moodleClient.deleteEvidence(msg.evidenceId)
            } catch (ignored: Exception) {}
        }
        messageDao.deleteMessageById(messageId)
        Result.success(Unit)
    }

    suspend fun clearChatHistory(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.updateLastMessage(chatId, "Historial vaciado", System.currentTimeMillis())
        Result.success(Unit)
    }

    suspend fun saveMoodleConfig(config: MoodleConfigEntity): Unit = withContext(Dispatchers.IO) {
        moodleConfigDao.saveConfig(config)
        moodleClient.configure(config.host, config.repoId)
    }

    suspend fun getUserByUsername(username: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username.trim().removePrefix("@"))
    }

    suspend fun getAllDirectoryUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        val cur = userDao.getCurrentUser()?.username ?: ""
        userDao.getAllUsersFlow().firstOrNull()?.filter { !it.username.equals(cur, ignoreCase = true) } ?: emptyList()
    }

    suspend fun getAllLocalAccounts(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getAllUsersFlow().firstOrNull() ?: emptyList()
    }

    suspend fun switchAccount(username: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@")
        val target = userDao.getUserByUsername(clean) ?: return@withContext Result.failure(IllegalArgumentException("Cuenta no encontrada"))
        userDao.clearCurrentUserFlag()
        val switched = target.copy(isCurrentUser = true, lastSeen = System.currentTimeMillis())
        userDao.insertOrUpdate(switched)
        syncRemoteEvidences()
        Result.success(switched)
    }

    suspend fun deleteAccount(username: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val target = username ?: userDao.getCurrentUser()?.username
        if (target != null) {
            userDao.deleteByUsername(target)
        }
        val remaining = userDao.getAllUsersFlow().firstOrNull()
        if (!remaining.isNullOrEmpty()) {
            val next = remaining.first().copy(isCurrentUser = true)
            userDao.insertOrUpdate(next)
        }
        Result.success(Unit)
    }

    suspend fun updateMemberRole(chatId: String, username: String, newRole: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            groupMemberDao.updateMemberRole(chatId, clean, newRole)
            Result.success(Unit)
        }

    suspend fun testServerLogin(username: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        val config = moodleConfigDao.getConfig() ?: MoodleConfigEntity()
        moodleClient.configure(config.host, config.repoId)
        val loginSuccess = moodleClient.login(username, pass)
        if (loginSuccess) {
            Result.success("Conexión con Moodle exitosa")
        } else {
            Result.failure(Exception("Error de autenticación en Moodle"))
        }
    }

    suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        userDao.clearCurrentUserFlag()
    }

    suspend fun updateCurrentUser(displayName: String, bio: String, avatarUrl: String) =
        withContext(Dispatchers.IO) {
            val current = userDao.getCurrentUser() ?: return@withContext
            val updated = current.copy(
                displayName = displayName,
                bio = bio,
                avatarUrl = avatarUrl,
                lastSeen = System.currentTimeMillis()
            )
            userDao.insertOrUpdate(updated)
        }
}
