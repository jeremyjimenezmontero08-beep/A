package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classmates")
data class Classmate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val personality: String = "",
    val hobby: String = "",
    val isFavorite: Boolean = false
)

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val classmateId: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val contentUri: String = "", // Picsum or simulated video url
    val caption: String = "",
    val isVideo: Boolean = false,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val likedBy: List<String> = emptyList()
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val classmateId: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val contentUri: String = "",
    val isVideo: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val classmateId: String = "", // The roommate buddy we chat with
    val isFromMe: Boolean = false,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
