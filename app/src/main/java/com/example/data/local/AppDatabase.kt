package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        MoodleConfigEntity::class,
        GroupMemberEntity::class,
        StatusEntity::class,
        BannedDeviceEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun moodleConfigDao(): MoodleConfigDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun statusDao(): StatusDao
    abstract fun bannedDeviceDao(): BannedDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    context.deleteDatabase("teleucf_database")
                    context.deleteDatabase("nexus_production_chat.db")
                    context.deleteDatabase("nexus_chat_clean_v3.db")
                } catch (ignored: Exception) {}

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chatpro_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
