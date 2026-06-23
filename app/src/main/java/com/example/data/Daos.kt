package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassmateDao {
    @Query("SELECT * FROM classmates ORDER BY name ASC")
    fun getAllClassmates(): Flow<List<Classmate>>

    @Query("SELECT * FROM classmates WHERE id = :id LIMIT 1")
    suspend fun getClassmateById(id: Int): Classmate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassmate(classmate: Classmate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassmates(classmates: List<Classmate>)

    @Update
    suspend fun updateClassmate(classmate: Classmate)

    @Query("DELETE FROM classmates WHERE id != 0")
    suspend fun deleteOtherClassmates()
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY dateTimestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Update
    suspend fun updatePost(post: Post)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: Int)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: Int)

    @Query("DELETE FROM stories")
    suspend fun deleteAllStories()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE classmateId = :classmateId ORDER BY timestamp ASC")
    fun getChatMessages(classmateId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE classmateId = :classmateId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForClassmate(classmateId: Int): ChatMessage?

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()
}
