package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Classmate::class, Post::class, Story::class, ChatMessage::class],
    version = 3,
    exportSchema = false
)
abstract class PromoDatabase : RoomDatabase() {
    abstract fun classmateDao(): ClassmateDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: PromoDatabase? = null

        fun getDatabase(context: Context): PromoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PromoDatabase::class.java,
                    "promo_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
