package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classmates")
data class Classmate(
    @PrimaryKey val id: Int, // 1 to 37 (Let's make 0 the current user)
    val name: String,
    val nickname: String,
    val avatarUrl: String,
    val bio: String,
    val personality: String,
    val hobby: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classmateId: Int,
    val authorName: String,
    val authorAvatar: String,
    val contentUri: String, // Picsum or simulated video url
    val caption: String,
    val isVideo: Boolean,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean = false
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classmateId: Int,
    val authorName: String,
    val authorAvatar: String,
    val contentUri: String,
    val isVideo: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classmateId: Int, // The roommate buddy we chat with
    val isFromMe: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
