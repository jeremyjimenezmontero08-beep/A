package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PromoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PromoDatabase.getDatabase(application)
    private val repository = PromoRepository(db)

    // UI Navigation Tab state
    private val _currentTab = MutableStateFlow(Tab.FEED)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    // Current active logged in user ID (Default is 0: Jeremy)
    private val _currentUserId = MutableStateFlow(0)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    // Selected classmate for Private Chat
    private val _selectedClassmateId = MutableStateFlow<Int?>(null)
    val selectedClassmateId: StateFlow<Int?> = _selectedClassmateId.asStateFlow()

    // Selected classmate for Profile view popup
    private val _viewedClassmateId = MutableStateFlow<Int?>(null)
    val viewedClassmateId: StateFlow<Int?> = _viewedClassmateId.asStateFlow()

    // Selected story to view fullscreen
    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    // Chat typing indicator state
    private val _isBuddyTyping = MutableStateFlow(false)
    val isBuddyTyping: StateFlow<Boolean> = _isBuddyTyping.asStateFlow()

    enum class Tab {
        FEED, CHATS, ADD, PROFILE
    }

    val classmates: StateFlow<List<Classmate>> = repository.classmates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val posts: StateFlow<List<Post>> = repository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<Story>> = repository.stories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observe active chat messages in real-time
    val activeChatMessages: StateFlow<List<ChatMessage>> = _selectedClassmateId
        .flatMapLatest { id ->
            if (id != null) repository.getChatMessages(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // One-time self-healing check: if old Sofía Martínez classmate exists from prior seeds, wipe database to start clean
            val oldSofia = db.classmateDao().getClassmateById(1)
            if (oldSofia != null && oldSofia.name == "Sofía Martínez") {
                db.classmateDao().deleteOtherClassmates()
                db.postDao().deleteAllPosts()
                db.storyDao().deleteAllStories()
                db.chatMessageDao().deleteAllChatMessages()
            }

            // First build baseline databases (Jeremy)
            repository.initializeDatabaseIfEmpty()
            
            // Validate classmate 0 creation for local user representation
            val hasUser = db.classmateDao().getClassmateById(0)
            if (hasUser == null) {
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

    fun changeTab(tab: Tab) {
        _currentTab.value = tab
        if (tab != Tab.CHATS) {
            _selectedClassmateId.value = null
        }
    }

    fun selectChat(classmateId: Int?) {
        _selectedClassmateId.value = classmateId
        if (classmateId != null) {
            _currentTab.value = Tab.CHATS
        }
    }

    fun viewClassmateProfile(classmateId: Int?) {
        _viewedClassmateId.value = classmateId
    }

    fun showStory(story: Story?) {
        _activeStory.value = story
    }

    fun toggleLike(post: Post) = viewModelScope.launch {
        val updatedPost = post.copy(
            isLiked = !post.isLiked,
            likesCount = if (post.isLiked) post.likesCount - 1 else post.likesCount + 1
        )
        repository.updatePost(updatedPost)
    }

    fun sendChatMessage(text: String) = viewModelScope.launch {
        val buddyId = _selectedClassmateId.value ?: return@launch
        if (text.isBlank()) return@launch

        // Persist User outbound chat bubbles
        val myMessage = ChatMessage(classmateId = buddyId, isFromMe = true, text = text)
        repository.insertChatMessage(myMessage)

        // Find classmate profile to shape responses
        val buddyProfile = classmates.value.find { it.id == buddyId } ?: return@launch

        // Trigger typing state to increase social visual realism
        _isBuddyTyping.value = true
        delay(1200)

        // Request custom chatbot flow through direct REST or offline heuristics
        val currentHistory = activeChatMessages.value
        val replyText = GeminiClient.generateClassmateReply(
            classmateName = buddyProfile.name,
            classmateNickname = buddyProfile.nickname,
            classmateBio = buddyProfile.bio,
            classmatePersonality = buddyProfile.personality,
            chatHistory = currentHistory,
            newMessage = text
        )

        val buddyMessage = ChatMessage(classmateId = buddyId, isFromMe = false, text = replyText)
        repository.insertChatMessage(buddyMessage)
        _isBuddyTyping.value = false
    }

    fun uploadPost(caption: String, imageUrl: String, isVideo: Boolean) = viewModelScope.launch {
        val me = classmates.value.find { it.id == _currentUserId.value } ?: Classmate(0, "Jeremy Jiménez", "Jeremy", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500", "Admin", "Admin", "Admin")
        val photoUrl = if (imageUrl.isBlank()) {
            val randomId = (10..400).random()
            "https://picsum.photos/id/$randomId/800/800"
        } else imageUrl

        val newPost = Post(
            classmateId = me.id,
            authorName = me.name,
            authorAvatar = me.avatarUrl,
            contentUri = photoUrl,
            caption = caption,
            isVideo = isVideo,
            likesCount = 0,
            commentsCount = 0
        )
        repository.insertPost(newPost)
        _currentTab.value = Tab.FEED
    }

    fun uploadStory(imageUrl: String, isVideo: Boolean) = viewModelScope.launch {
        val me = classmates.value.find { it.id == _currentUserId.value } ?: Classmate(0, "Jeremy Jiménez", "Jeremy", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500", "Admin", "Admin", "Admin")
        val storyUrl = if (imageUrl.isBlank()) {
            val randomId = (10..400).random()
            "https://picsum.photos/id/$randomId/600/1000"
        } else imageUrl

        val newStory = Story(
            classmateId = me.id,
            authorName = me.name,
            authorAvatar = me.avatarUrl,
            contentUri = storyUrl,
            isVideo = isVideo
        )
        repository.insertStory(newStory)
        _currentTab.value = Tab.FEED
    }

    fun updateMyProfile(name: String, nickname: String, bio: String, avatar: String) = viewModelScope.launch {
        val me = classmates.value.find { it.id == _currentUserId.value } ?: return@launch
        val updated = me.copy(
            name = name.ifBlank { me.name },
            nickname = nickname.ifBlank { me.nickname },
            bio = bio.ifBlank { me.bio },
            avatarUrl = avatar.ifBlank { me.avatarUrl }
        )
        repository.updateClassmate(updated)
    }

    fun registerNewClassmate(
        name: String,
        nickname: String,
        bio: String,
        avatar: String,
        personality: String,
        hobby: String
    ) = viewModelScope.launch {
        val list = classmates.value
        val nextId = (list.map { it.id }.maxOrNull() ?: 0) + 1
        val finalAvatar = avatar.ifBlank {
            "https://picsum.photos/id/${(20..250).random()}/200/200"
        }
        val classmate = Classmate(
            id = nextId,
            name = name,
            nickname = nickname,
            avatarUrl = finalAvatar,
            bio = bio,
            personality = personality.ifBlank { "Amable, alegre y conversador" },
            hobby = hobby.ifBlank { "Socializar" }
        )
        db.classmateDao().insertClassmates(listOf(classmate))
    }

    fun switchActiveUser(userId: Int) {
        _currentUserId.value = userId
        _selectedClassmateId.value = null
        _viewedClassmateId.value = null
    }
}
