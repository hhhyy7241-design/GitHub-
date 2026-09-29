package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Query("UPDATE users SET isCurrentUser = 0 WHERE isCurrentUser = 1")
    suspend fun clearCurrentUserFlag()

    @Query("DELETE FROM users WHERE LOWER(username) = LOWER(:username)")
    suspend fun deleteByUsername(username: String)

    @Query("UPDATE users SET lastSeen = :lastSeen WHERE LOWER(username) = LOWER(:username)")
    suspend fun updateLastSeen(username: String, lastSeen: Long)
}

@Dao
interface BannedDeviceDao {
    @Query("SELECT * FROM banned_devices")
    fun getAllBannedDevicesFlow(): Flow<List<BannedDeviceEntity>>

    @Query("SELECT * FROM banned_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getBannedDevice(deviceId: String): BannedDeviceEntity?

    @Query("SELECT * FROM banned_devices WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getBannedByUsername(username: String): BannedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(banned: BannedDeviceEntity)

    @Query("DELETE FROM banned_devices WHERE deviceId = :deviceId")
    suspend fun delete(deviceId: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY pinned DESC, lastMessageTime DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    fun getChatByIdFlow(id: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    suspend fun getChatById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatEntity>)

    @Query("UPDATE chats SET lastMessageSnippet = :snippet, lastMessageTime = :time, lastMessageSenderId = :senderId WHERE id = :id")
    suspend fun updateLastMessage(id: String, snippet: String, time: Long, senderId: String = "")

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :id")
    suspend fun updateUnreadCount(id: String, count: Int)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChatById(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("UPDATE messages SET status = 'READ' WHERE chatId = :chatId AND isOutgoing = 0 AND status != 'READ'")
    suspend fun markMessagesAsRead(chatId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("UPDATE messages SET attachmentUrl = NULL, attachmentName = NULL, attachmentType = NULL WHERE id = :id")
    suspend fun clearAttachment(id: String)
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun getActiveStatusesFlow(now: Long): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE id = :id LIMIT 1")
    suspend fun getStatusById(id: String): StatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: StatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<StatusEntity>)

    @Query("DELETE FROM statuses WHERE id = :id")
    suspend fun deleteStatus(id: String)

    @Query("DELETE FROM statuses WHERE expiresAt <= :now")
    suspend fun cleanupExpiredStatuses(now: Long)
}

@Dao
interface MoodleConfigDao {
    @Query("SELECT * FROM server_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<MoodleConfigEntity?>

    @Query("SELECT * FROM server_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): MoodleConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: MoodleConfigEntity)
}

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members")
    fun getAllGroupMembersFlow(): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE LOWER(username) = LOWER(:username)")
    fun getUserGroupsFlow(username: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE chatId = :chatId ORDER BY CASE role WHEN 'OWNER' THEN 1 WHEN 'ADMIN' THEN 2 ELSE 3 END, username ASC")
    fun getMembersForChatFlow(chatId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE chatId = :chatId AND LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getMember(chatId: String, username: String): GroupMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE chatId = :chatId AND LOWER(username) = LOWER(:username)")
    suspend fun removeMember(chatId: String, username: String)

    @Query("DELETE FROM group_members WHERE chatId = :chatId")
    suspend fun removeAllMembersForChat(chatId: String)

    @Query("UPDATE group_members SET role = :newRole WHERE chatId = :chatId AND LOWER(username) = LOWER(:username)")
    suspend fun updateMemberRole(chatId: String, username: String, newRole: String)
}
