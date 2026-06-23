package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PromoRepository(private val db: PromoDatabase) {
    val classmates: Flow<List<Classmate>> = db.classmateDao().getAllClassmates()
    val posts: Flow<List<Post>> = db.postDao().getAllPosts()
    val stories: Flow<List<Story>> = db.storyDao().getAllStories()

    fun getChatMessages(classmateId: Int): Flow<List<ChatMessage>> {
        return db.chatMessageDao().getChatMessages(classmateId)
    }

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        db.chatMessageDao().insertChatMessage(message)
    }

    suspend fun insertPost(post: Post) = withContext(Dispatchers.IO) {
        db.postDao().insertPost(post)
    }

    suspend fun updatePost(post: Post) = withContext(Dispatchers.IO) {
        db.postDao().updatePost(post)
    }

    suspend fun insertStory(story: Story) = withContext(Dispatchers.IO) {
        db.storyDao().insertStory(story)
    }

    suspend fun updateClassmate(classmate: Classmate) = withContext(Dispatchers.IO) {
        db.classmateDao().updateClassmate(classmate)
    }

    suspend fun getLastMessageForClassmate(classmateId: Int): ChatMessage? = withContext(Dispatchers.IO) {
        db.chatMessageDao().getLastMessageForClassmate(classmateId)
    }

    suspend fun initializeDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val currentClassmates = db.classmateDao().getAllClassmates().first()
        if (currentClassmates.isEmpty()) {
            db.classmateDao().insertClassmates(listOf(
                Classmate(
                    id = 0,
                    name = "Jeremy Jiménez",
                    nickname = "Jeremy",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500",
                    bio = "Programador de ENSH 💻💥. ¡Un saludo!",
                    personality = "Creativo, chistoso y gran dador de consejos tecnológicos",
                    hobby = "Programación"
                )
            ))
        }
    }
}
