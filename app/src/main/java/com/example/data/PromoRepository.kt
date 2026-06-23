package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PromoRepository(private val dbFallback: PromoDatabase) {
    private val firestore = FirebaseFirestore.getInstance()

    val classmates: Flow<List<Classmate>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                snapshot?.let {
                    val users = it.documents.mapNotNull { doc -> 
                        try {
                            doc.toObject(Classmate::class.java)
                        } catch (e: Exception) { null }
                    }
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    val posts: Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .orderBy("dateTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                snapshot?.let {
                    val postsList = it.documents.mapNotNull { doc -> 
                        try {
                            doc.toObject(Post::class.java)
                        } catch (e: Exception) { null }
                    }
                    trySend(postsList)
                }
            }
        awaitClose { listener.remove() }
    }

    val stories: Flow<List<Story>> = callbackFlow {
        val listener = firestore.collection("stories")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                snapshot?.let {
                    val storiesList = it.documents.mapNotNull { doc -> 
                        try {
                            doc.toObject(Story::class.java)
                        } catch (e: Exception) { null }
                    }
                    trySend(storiesList)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getChatMessages(classmateId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereEqualTo("classmateId", classmateId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                snapshot?.let {
                    val messages = it.documents.mapNotNull { doc -> 
                        try {
                            doc.toObject(ChatMessage::class.java)
                        } catch (e: Exception) { null }
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        try { firestore.collection("chats").document(message.id).set(message).await() } catch (e: Exception) {}
    }

    suspend fun insertPost(post: Post) = withContext(Dispatchers.IO) {
        try { firestore.collection("posts").document(post.id).set(post).await() } catch (e: Exception) {}
    }

    suspend fun updatePost(post: Post) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("posts").document(post.id).set(post).await()
        } catch (e: Exception) {}
    }

    suspend fun insertStory(story: Story) = withContext(Dispatchers.IO) {
        try { firestore.collection("stories").document(story.id).set(story).await() } catch (e: Exception) {}
    }

    suspend fun insertClassmate(classmate: Classmate): String = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.collection("users").document(classmate.id).set(classmate).await()
            classmate.id
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun updateClassmate(classmate: Classmate) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(classmate.id).set(classmate).await()
        } catch (e: Exception) {}
    }

    suspend fun getLastMessageForClassmate(classmateId: String): ChatMessage? = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = firestore.collection("chats")
                .whereEqualTo("classmateId", classmateId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
            result.documents.firstOrNull()?.toObject(ChatMessage::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun initializeDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val check = firestore.collection("users").limit(1).get().await()
            if (check.isEmpty) {
                val initUser = Classmate(
                    name = "Jeremy Jiménez",
                    nickname = "Jeremy",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500",
                    bio = "Programador de ENSH 💻💥. ¡Un saludo!",
                    personality = "Creativo, chistoso y gran dador de consejos tecnológicos",
                    hobby = "Programación"
                )
                firestore.collection("users").document(initUser.id).set(initUser).await()
            }
        } catch (e: Exception) {}
    }
}
