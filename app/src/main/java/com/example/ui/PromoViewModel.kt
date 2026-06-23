package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PromoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PromoDatabase.getDatabase(application)
    private val repository = PromoRepository(db)

    // UI Navigation Tab state
    private val _currentTab = MutableStateFlow(Tab.FEED)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    // Current active logged in user ID ("" means not logged in)
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // Are we showing login screen?
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Selected classmate for Private Chat
    private val _selectedClassmateId = MutableStateFlow<String?>(null)
    val selectedClassmateId: StateFlow<String?> = _selectedClassmateId.asStateFlow()

    // Selected classmate for Profile view popup
    private val _viewedClassmateId = MutableStateFlow<String?>(null)
    val viewedClassmateId: StateFlow<String?> = _viewedClassmateId.asStateFlow()

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
            repository.initializeDatabaseIfEmpty()
        }
    }

    fun setCurrentUser(userId: String) {
        _currentUserId.value = userId
        _isUserLoggedIn.value = userId.isNotBlank()
    }

    fun registerNewUser(name: String, nickname: String, bio: String) {
        viewModelScope.launch {
            val freshId = repository.insertClassmate(
                Classmate(
                    name = name,
                    nickname = nickname,
                    avatarUrl = "default_blank",
                    bio = bio,
                    personality = "Soy nuevo por aquí",
                    hobby = "Descubrir"
                )
            )
            setCurrentUser(freshId)
        }
    }

    fun logout() {
        _currentUserId.value = ""
        _isUserLoggedIn.value = false
    }

    fun changeTab(tab: Tab) {
        _currentTab.value = tab
        if (tab != Tab.CHATS) {
            _selectedClassmateId.value = null
        }
    }

    fun selectChat(classmateId: String?) {
        _selectedClassmateId.value = classmateId
        if (classmateId != null) {
            _currentTab.value = Tab.CHATS
        }
    }

    fun viewClassmateProfile(classmateId: String?) {
        _viewedClassmateId.value = classmateId
    }

    fun showStory(story: Story?) {
        _activeStory.value = story
    }

    fun toggleLike(post: Post) = viewModelScope.launch {
        val currentUserId = _currentUserId.value
        if (currentUserId.isBlank()) return@launch

        val isCurrentlyLiked = post.likedBy.contains(currentUserId)
        
        val newLikedBy = if (isCurrentlyLiked) {
            post.likedBy - currentUserId
        } else {
            post.likedBy + currentUserId
        }
        
        val updatedPost = post.copy(
            likedBy = newLikedBy,
            likesCount = newLikedBy.size
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
        val me = classmates.value.find { it.id == _currentUserId.value } ?: Classmate(name = "Jeremy Jiménez", nickname = "Jeremy", avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500")
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
        val me = classmates.value.find { it.id == _currentUserId.value } ?: Classmate(name = "Jeremy Jiménez", nickname = "Jeremy", avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500")
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
        val finalAvatar = avatar.ifBlank {
            "default_blank"
        }
        val classmate = Classmate(
            name = name,
            nickname = nickname,
            avatarUrl = finalAvatar,
            bio = bio,
            personality = personality.ifBlank { "Amable, alegre y conversador" },
            hobby = hobby.ifBlank { "Socializar" }
        )
        repository.insertClassmate(classmate)
    }

    fun switchActiveUser(userId: String) {
        _currentUserId.value = userId
        _selectedClassmateId.value = null
        _viewedClassmateId.value = null
    }
}
