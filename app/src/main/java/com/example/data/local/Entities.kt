package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarUrl: String = "",
    val isCurrentUser: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val password: String = "",
    val role: String = "MEMBER", // "OWNER", "ADMIN", "MEMBER"
    val deviceId: String = ""
) {
    val isOnline: Boolean
        get() = isCurrentUser
}

@Entity(tableName = "banned_devices")
data class BannedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val username: String = "",
    val reason: String = "Expulsado y bloqueado permanentemente por el Administrador",
    val bannedAt: Long = System.currentTimeMillis(),
    val evidenceId: String? = null
)

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatar: String = "",
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaType: String = "IMAGE", // "IMAGE", "VIDEO"
    val backgroundColorHex: String = "#0284C7",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L, // 24 hours
    val evidenceId: String? = null
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "DIRECT" or "GROUP"
    val avatarUrl: String = "",
    val description: String = "",
    val unreadCount: Int = 0,
    val lastMessageSnippet: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String = "",
    val pinned: Boolean = false,
    val remoteSyncUrl: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val text: String = "",
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null, // "IMAGE", "DOCUMENT", "AUDIO", "VIDEO", "OTHER"
    val attachmentSize: Long = 0L, // Size in bytes
    val isOutgoing: Boolean = true,
    val status: String = "SENT", // "PENDING", "SENT", "DELIVERED", "READ"
    val evidenceId: String? = null,
    val isEdited: Boolean = false,
    val replyToId: String? = null,
    val replyToSender: String? = null,
    val replyToText: String? = null
)

@Entity(tableName = "server_config")
data class MoodleConfigEntity(
    @PrimaryKey val id: Int = 1,
    val host: String = "https://cursos.ucf.edu.cu/",
    val username: String = "julianrene",
    val password: String = "Transfer60*",
    val repoId: Int = 4,
    val uploadType: String = "evidence",
    val maxChunkBytes: Long = 4 * 1024 * 1024L,
    val sesskey: String? = null,
    val token: String? = null,
    val lastConnectionStatus: String? = "Listo",
    val lastConnectionTime: Long = 0L
)

@Entity(
    tableName = "group_members",
    primaryKeys = ["chatId", "username"]
)
data class GroupMemberEntity(
    val chatId: String,
    val username: String,
    val role: String = "MEMBER", // "OWNER", "ADMIN", "MEMBER"
    val joinedAt: Long = System.currentTimeMillis()
)

data class GroupMemberWithUser(
    val member: GroupMemberEntity,
    val user: UserEntity?
)
