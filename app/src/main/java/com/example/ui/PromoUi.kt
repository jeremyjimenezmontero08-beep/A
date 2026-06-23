package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Classmate
import com.example.data.ChatMessage
import com.example.data.Post
import com.example.data.Story
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Beautiful Dark Cosmic Color Palette
val CosmicBackground = Color(0xFF090615)
val CosmicSurface = Color(0xFF140F26)
val CosmicSurfaceLighter = Color(0xFF1B1530)
val NeonPink = Color(0xFFFF2F7D)
val NeonCyan = Color(0xFF00FFCC)
val ElectricViolet = Color(0xFF8B5CF6)
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

@Composable
fun AvatarImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (model.isNullOrBlank() || model == "default_blank") {
        Box(
            modifier = modifier.background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = Color.Black,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
fun PromoMainScreen(viewModel: PromoViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val classmates by viewModel.classmates.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val selectedClassmateId by viewModel.selectedClassmateId.collectAsState()
    val viewedClassmateId by viewModel.viewedClassmateId.collectAsState()
    val activeStory by viewModel.activeStory.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val me = classmates.find { it.id == currentUserId }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        bottomBar = {
            if (selectedClassmateId == null) {
                PromoBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.changeTab(it) }
                )
            }
        },
        containerColor = CosmicBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Subtle atmospheric neon violet glows in background corners
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricViolet.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        radius = size.width,
                        center = androidx.compose.ui.geometry.Offset(0f, 0f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonPink.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        radius = size.width * 0.8f,
                        center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.8f)
                    )
                }
        ) {
            when (currentTab) {
                PromoViewModel.Tab.FEED -> {
                    FeedScreen(
                        stories = stories,
                        posts = posts,
                        classmates = classmates,
                        viewModel = viewModel
                    )
                }
                PromoViewModel.Tab.CHATS -> {
                    if (selectedClassmateId != null) {
                        val buddy = classmates.find { it.id == selectedClassmateId }
                        if (buddy != null) {
                            PrivateChatScreen(
                                buddy = buddy,
                                viewModel = viewModel
                            )
                        } else {
                            viewModel.selectChat(null)
                        }
                    } else {
                        ChatsBoardScreen(
                            classmates = classmates,
                            viewModel = viewModel
                        )
                    }
                }
                PromoViewModel.Tab.ADD -> {
                    AddPublishScreen(viewModel = viewModel)
                }
                PromoViewModel.Tab.PROFILE -> {
                    MyProfileScreen(
                        me = me,
                        posts = posts.filter { it.classmateId == (me?.id ?: 0) },
                        viewModel = viewModel
                    )
                }
            }

            // Display Fullscreen active story if any
            if (activeStory != null) {
                StoryViewerDialog(
                    story = activeStory!!,
                    onDismiss = { viewModel.showStory(null) }
                )
            }

            // Display Clicked Classmate Profile Sheet if any
            if (viewedClassmateId != null) {
                val buddy = classmates.find { it.id == viewedClassmateId }
                if (buddy != null) {
                    ClassmateProfileDialog(
                        classmate = buddy,
                        viewModel = viewModel,
                        onDismiss = { viewModel.viewClassmateProfile(null) }
                    )
                } else {
                    viewModel.viewClassmateProfile(null)
                }
            }
        }
    }
}

@Composable
fun PromoBottomBar(
    currentTab: PromoViewModel.Tab,
    onTabSelected: (PromoViewModel.Tab) -> Unit
) {
    NavigationBar(
        containerColor = CosmicSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("app_bottom_bar")
    ) {
        val tabs = listOf(
            Triple(PromoViewModel.Tab.FEED, Icons.Default.Home, "Feed"),
            Triple(PromoViewModel.Tab.CHATS, Icons.Default.Chat, "Chats"),
            Triple(PromoViewModel.Tab.ADD, Icons.Default.AddBox, "Subir"),
            Triple(PromoViewModel.Tab.PROFILE, Icons.Default.Person, "Perfil")
        )

        tabs.forEach { (tab, icon, label) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) NeonPink else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CosmicSurfaceLighter
                )
            )
        }
    }
}

// ==================== FEED SCREEN ====================

@Composable
fun FeedScreen(
    stories: List<Story>,
    posts: List<Post>,
    classmates: List<Classmate>,
    viewModel: PromoViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // App Header Brand
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ENSH",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Nuestra Red Social Privada 🎓🏫",
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = { viewModel.changeTab(PromoViewModel.Tab.CHATS) }) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Chats",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    // Pulse dot for chat notification
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .background(NeonPink, CircleShape)
                            .border(1.5.dp, CosmicBackground, CircleShape)
                    )
                }
            }
        }

        // Horizontal Stories Row
        StoriesRow(stories = stories, classmates = classmates, viewModel = viewModel)

        Divider(
            color = CosmicSurfaceLighter,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Vertical Feed Posts View
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "No Posts",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sin publicaciones todavía",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sé el primero de tu curso en subir una foto o video.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_lazy_column"),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(posts) { post ->
                    val authorNode = classmates.find { it.id == post.classmateId }
                    PostItemCard(post = post, author = authorNode, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StoriesRow(
    stories: List<Story>,
    classmates: List<Classmate>,
    viewModel: PromoViewModel
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("stories_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Option to add story for current user (ID 0)
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.changeTab(PromoViewModel.Tab.ADD) }
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(listOf(TextSecondary, CosmicSurface)),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        AvatarImage(
                            model = viewModel.currentUserId.value.let { id -> classmates.find { it.id == id }?.avatarUrl } ?: "default_blank",
                            contentDescription = "My Story Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(NeonCyan, CircleShape)
                            .border(2.dp, CosmicBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = CosmicBackground,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu Historia",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Classmate active stories
        items(stories) { story ->
            val cl = classmates.find { it.id == story.classmateId }
            val nickname = cl?.nickname ?: story.authorName
            val brush = Brush.sweepGradient(listOf(NeonPink, NeonCyan, NeonPink))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.showStory(story) }
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .border(
                            width = 2.5.dp,
                            brush = if (story.isViewed) Brush.linearGradient(listOf(TextSecondary, TextSecondary)) else brush,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                ) {
                    AvatarImage(
                        model = story.authorAvatar,
                        contentDescription = story.authorName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nickname,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostItemCard(
    post: Post,
    author: Classmate?,
    viewModel: PromoViewModel
) {
    var showCommentBox by remember { mutableStateOf(false) }
    var simpleCommentText by remember { mutableStateOf("") }
    val localComments = remember { mutableStateListOf<String>() }

    // Coroutine scope for animations
    val scope = rememberCoroutineScope()
    var isDoubleTapHeartVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CosmicSurfaceLighter)
    ) {
        Column {
            // Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.viewClassmateProfile(post.classmateId)
                    }
                ) {
                    Box {
                        AvatarImage(
                            model = post.authorAvatar,
                            contentDescription = post.authorName,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        )
                        // Online indicator dot
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .background(Color(0xFF4ADE80), CircleShape)
                                .align(Alignment.BottomEnd)
                                .border(1.5.dp, CosmicSurface, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (post.classmateId == viewModel.currentUserId.value) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Tú",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = (author?.nickname?.let { "@$it" } ?: "Compañero") + (if (author?.hobby != null) " • ${author.hobby}" else ""),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = { viewModel.viewClassmateProfile(post.classmateId) }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ver Perfil",
                        tint = TextSecondary
                    )
                }
            }

            // Post Visual Media Container with Double Tap to Like
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .combinedClickable(
                        onDoubleClick = {
                            val currentUserId = viewModel.currentUserId.value
                            if (!post.likedBy.contains(currentUserId)) {
                                viewModel.toggleLike(post)
                            }
                            isDoubleTapHeartVisible = true
                            scope.launch {
                                delay(800)
                                isDoubleTapHeartVisible = false
                            }
                        },
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = post.contentUri,
                    contentDescription = "Post Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // TikTok Simulated Video controls overlay
                if (post.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                    )
                    // Video Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video",
                                tint = NeonPink,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Video Loop",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Rotating record icon
                    val infiniteTransition = rememberInfiniteTransition()
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio track",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .rotate(angle)
                    )
                    
                    // Video progress loop bar
                    val progVal by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                    LinearProgressIndicator(
                        progress = { progVal },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = NeonPink,
                        trackColor = Color.Transparent,
                    )
                }

                // Super nice scaling double tap heart animation overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isDoubleTapHeartVisible,
                    enter = scaleIn(animationSpec = spring(dampingRatio = 0.4f)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Double tap heart",
                        tint = NeonPink,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            // Interactive Action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentUserId = viewModel.currentUserId.value
                    val isLikedByMe = post.likedBy.contains(currentUserId)
                    IconButton(onClick = { viewModel.toggleLike(post) }) {
                        Icon(
                            imageVector = if (isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLikedByMe) NeonPink else TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = { showCommentBox = !showCommentBox }) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "Comment",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${post.commentsCount + localComments.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = { /* Simulated native share popup */ }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Caption Text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = post.authorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = post.caption,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Expandable Comment List & Input Tray
            AnimatedVisibility(visible = showCommentBox) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .background(CosmicSurfaceLighter, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Comentarios del curso:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Default static comment mocks with teenager vibe
                    Text(
                        text = "💬 Sofia: ¡Buenísima foto! 🔥🙌",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "💬 Mateo: Buuena crack!! 🛹⚡",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Render interactive custom user comments
                    localComments.forEach { comment ->
                        Text(
                            text = "💬 Tú: $comment",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comment text box field
                    OutlinedTextField(
                        value = simpleCommentText,
                        onValueChange = { simpleCommentText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = { Text("Escribe un comentario...", fontSize = 11.sp, color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CosmicSurface,
                            focusedContainerColor = CosmicBackground,
                            unfocusedContainerColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (simpleCommentText.isNotBlank()) {
                                    localComments.add(simpleCommentText.trim())
                                    simpleCommentText = ""
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ==================== STORY VIEWER ====================

@Composable
fun StoryViewerDialog(
    story: Story,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .testTag("story_viewer_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Fullscreen image
                AsyncImage(
                    model = story.contentUri,
                    contentDescription = "Story Content View",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top Progress and User info overlays
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .padding(14.dp)
                ) {
                    // Running progress animation line
                    var progress by remember { mutableStateOf(0f) }
                    LaunchedEffect(key1 = true) {
                        val duration = 4000
                        val step = 40
                        var passed = 0
                        while (passed < duration) {
                            delay(step.toLong())
                            passed += step
                            progress = passed.toFloat() / duration.toFloat()
                        }
                        onDismiss() // Close when running completes
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = NeonPink,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarImage(
                                model = story.authorAvatar,
                                contentDescription = story.authorName,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = story.authorName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Story",
                                tint = Color.White
                            )
                        }
                    }
                }

                // TikTok music label simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Song track",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sonido original (ENSH) 🎶✨",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ==================== CHATS BOARD SCREEN ====================

@Composable
fun ChatsBoardScreen(
    classmates: List<Classmate>,
    viewModel: PromoViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredClassmates = classmates.filter {
        it.id != viewModel.currentUserId.value && (it.name.contains(searchQuery, ignoreCase = true) || it.nickname.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Amigos de ENSH",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        Text(
            text = "Conecta y chatea en privado con tus amigos de ENSH.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // Sleek Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Buscar compañeros por nombre o apodo...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, "Buscar", tint = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CosmicSurface,
                focusedContainerColor = CosmicSurface,
                unfocusedContainerColor = CosmicSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chats_board_lazy_column"),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredClassmates) { buddy ->
                ClassmateChatRow(buddy = buddy, onChatOpen = { viewModel.selectChat(buddy.id) }) {
                    viewModel.viewClassmateProfile(buddy.id)
                }
            }
        }
    }
}

@Composable
fun ClassmateChatRow(
    buddy: Classmate,
    onChatOpen: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CosmicSurfaceLighter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileClick() }
            ) {
                Box {
                    AvatarImage(
                        model = buddy.avatarUrl,
                        contentDescription = buddy.name,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4ADE80), CircleShape)
                            .align(Alignment.BottomEnd)
                            .border(2.dp, CosmicSurface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buddy.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "@${buddy.nickname}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = buddy.bio,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onChatOpen,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("chat_with_${buddy.id}"),
                contentPadding = PaddingValues(horizontal = 14.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== PRIVATE CHAT SCREEN ====================

@Composable
fun PrivateChatScreen(
    buddy: Classmate,
    viewModel: PromoViewModel
) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val isBuddyTyping by viewModel.isBuddyTyping.collectAsState()
    var textMessage by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Scroll to the bottom on new messages
    LaunchedEffect(key1 = messages.size, key2 = isBuddyTyping) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header with buddy info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicSurface)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectChat(null) }) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
            }

            Box(modifier = Modifier.clickable { viewModel.viewClassmateProfile(buddy.id) }) {
                AvatarImage(
                    model = buddy.avatarUrl,
                    contentDescription = buddy.name,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF4ADE80), CircleShape)
                        .align(Alignment.BottomEnd)
                        .border(1.5.dp, CosmicSurface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.viewClassmateProfile(buddy.id) }
            ) {
                Text(
                    text = buddy.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isBuddyTyping) "Escribiendo..." else "En línea • @${buddy.nickname}",
                    fontSize = 11.sp,
                    color = if (isBuddyTyping) NeonCyan else Color(0xFF4ADE80),
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = { viewModel.viewClassmateProfile(buddy.id) }) {
                Icon(Icons.Default.Info, "Detalles", tint = NeonCyan)
            }
        }

        // Chat Message Thread
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💬 Chat Privado",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Envía un mensaje para iniciar la conversación con ${buddy.nickname}.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_lazy_column"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(message = msg)
                    }

                    if (isBuddyTyping) {
                        item {
                            TypingBubble(buddyNickname = buddy.nickname)
                        }
                    }
                }
            }
        }

        // Bottom Message Input Box
        val keyboardController = LocalSoftwareKeyboardController.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textMessage,
                onValueChange = { textMessage = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = { Text("Escribe un mensaje privado...", color = TextSecondary, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = CosmicSurfaceLighter,
                    focusedContainerColor = CosmicBackground,
                    unfocusedContainerColor = CosmicBackground
                ),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textMessage.isNotBlank()) {
                        viewModel.sendChatMessage(textMessage.trim())
                        textMessage = ""
                        keyboardController?.hide()
                    }
                })
            )

            Spacer(modifier = Modifier.width(6.dp))

            FloatingActionButton(
                onClick = {
                    if (textMessage.isNotBlank()) {
                        viewModel.sendChatMessage(textMessage.trim())
                        textMessage = ""
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .testTag("chat_send_button"),
                containerColor = ElectricViolet,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isFromMe) Brush.linearGradient(listOf(NeonPink, ElectricViolet)) else Brush.linearGradient(listOf(CosmicSurfaceLighter, CosmicSurfaceLighter))
    val textColor = TextPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromMe) 16.dp else 2.dp,
                bottomEnd = if (message.isFromMe) 2.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TypingBubble(buddyNickname: String) {
    // Dynamic loading bouncing dots logic
    val infiniteTransition = rememberInfiniteTransition()
    val dotCount = 3
    val dots = List(dotCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0f at (index * 150) with LinearEasing
                    0.6f at (index * 150 + 200) with LinearEasing
                    0f at (index * 150 + 400) with LinearEasing
                },
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceLighter),
            modifier = Modifier.widthIn(max = 140.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$buddyNickname está escribiendo",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row {
                    dots.forEach { dot ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .offset(y = (-5 * dot.value).dp)
                                .background(NeonCyan, CircleShape)
                                .padding(horizontal = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== ADD PUBLISH SCREEN ====================

@Composable
fun AddPublishScreen(viewModel: PromoViewModel) {
    var caption by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isVideo by remember { mutableStateOf(false) }
    var uploadType by remember { mutableStateOf("POST") } // POST or STORY
    
    val context = LocalContext.current
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }
    
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            imageUrl = tempCameraUri.toString()
        }
    }
    
    fun createTempImageUri(): android.net.Uri {
        val file = java.io.File(context.cacheDir, "temp_camera_photo_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Curated pre-selected high-school aesthetics image list
    val templates = listOf(
        Pair("Fiesta", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500"),
        Pair("Clase", "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=500"),
        Pair("Fútbol", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=500"),
        Pair("Estilo", "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=500"),
        Pair("Estudio", "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=500")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CosmicBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Nueva Publicación",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Sube historias efímeras o posts al muro de ENSH.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Upload Type Select Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurface, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uploadType == "POST") ElectricViolet else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { uploadType = "POST" }
                ) {
                    Text(
                        text = "Post para Muro",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uploadType == "STORY") NeonPink else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { uploadType = "STORY" }
                ) {
                    Text(
                        text = "Historia Activa",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caption details
            if (uploadType == "POST") {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_caption_field"),
                    label = { Text("¿Qué está pasando hoy, colega?", color = TextSecondary) },
                    placeholder = { Text("Escribe una descripción genial para tu curso...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CosmicSurfaceLighter,
                        focusedContainerColor = CosmicSurface,
                        unfocusedContainerColor = CosmicSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Image input options
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val uri = createTempImageUri()
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceLighter),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Cámara", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cámara", color = TextPrimary, fontSize = 12.sp)
                }
                
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceLighter),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = NeonPink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galería", color = TextPrimary, fontSize = 12.sp)
                }
            }

            // Image URL Field input
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_image_url"),
                label = { Text("Enlace o ruta de la imagen (Opcional)", color = TextSecondary) },
                placeholder = { Text("Se llenará automáticamente o puedes pegar URL", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CosmicSurfaceLighter,
                    focusedContainerColor = CosmicSurface,
                    unfocusedContainerColor = CosmicSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fast templates selections
            Text(
                text = "Pre-visualiza fotos estéticas del colegio:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates) { pair ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurfaceLighter),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { imageUrl = pair.second }
                    ) {
                        Text(
                            text = "📸 ${pair.first}",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Media Type select (Photo or simulated video)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Formato TikTok (Simular Video Loop)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Agrega controles de video interactivo y barra de música.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isVideo,
                    onCheckedChange = { isVideo = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = ElectricViolet
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    scope.launch {
                        if (uploadType == "POST") {
                            viewModel.uploadPost(caption, imageUrl, isVideo)
                            snackbarHostState.showSnackbar("¡Publicado en el muro del curso! 🚀")
                        } else {
                            viewModel.uploadStory(imageUrl, isVideo)
                            snackbarHostState.showSnackbar("¡Historia subida! Mírala arriba 🌟")
                        }
                        caption = ""
                        imageUrl = ""
                        isVideo = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_publish_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uploadType == "POST") ElectricViolet else NeonPink
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (uploadType == "POST") "Publicar post en Muro" else "Subir como Historia",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==================== MY PROFILE SCREEN ====================

@Composable
fun MyProfileScreen(
    me: Classmate?,
    posts: List<Post>,
    viewModel: PromoViewModel
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    val classmates by viewModel.classmates.collectAsState()
    val stories by viewModel.stories.collectAsState()
    
    var editName by remember { mutableStateOf(me?.name ?: "") }
    var editNickname by remember { mutableStateOf(me?.nickname ?: "") }
    var editBio by remember { mutableStateOf(me?.bio ?: "") }
    var editAvatar by remember { mutableStateOf(me?.avatarUrl ?: "") }
    
    // Refresh state when "me" changes
    LaunchedEffect(me) {
        if (!showEditDialog) {
            editName = me?.name ?: ""
            editNickname = me?.nickname ?: ""
            editBio = me?.bio ?: ""
            editAvatar = me?.avatarUrl ?: ""
        }
    }

    val context = LocalContext.current
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            editAvatar = uri.toString()
        }
    }
    
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            editAvatar = tempCameraUri.toString()
        }
    }
    
    fun createTempImageUri(): android.net.Uri {
        val file = java.io.File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // High impact School banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricViolet, NeonPink, CosmicBackground)
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, CosmicBackground)
                        )
                    )
            )
        }

        // User profile Card info details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rounded Avatar overlapping the banner
            Box(
                modifier = Modifier
                    .offset(y = (-45).dp)
                    .size(90.dp)
                    .border(3.dp, CosmicBackground, CircleShape)
                    .shadow(elevation = 10.dp, shape = CircleShape)
            ) {
                AvatarImage(
                    model = me?.avatarUrl ?: "default_blank",
                    contentDescription = "My Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-35).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = me?.name ?: "Jeremy Jiménez",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "@${me?.nickname ?: "Jeremy"}",
                    fontSize = 13.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = me?.bio ?: "¡Hola! Probando la red social ENSH.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Course Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val myPostsCount = posts.count { it.classmateId == (me?.id ?: 0) }
                    val myStoriesCount = stories.count { it.classmateId == (me?.id ?: 0) }
                    ProfileStatItem(number = "${(classmates.size - 1).coerceAtLeast(0)}", label = "Amigos")
                    ProfileStatItem(number = "$myPostsCount", label = "Tus Posts")
                    ProfileStatItem(number = "$myStoriesCount", label = "Historias")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful accounts panel inside Profile Screen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicSurfaceLighter)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cuentas de ENSH", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            
                            // Button to register a classmate profile
                            TextButton(
                                onClick = { showRegisterDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                            ) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar Amigo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text(
                            text = "Agrega a tus amigos de curso para que tengan su cuenta en la app. Luego pulsa en cualquiera para cambiar de perfil activo y simular su actividad o publicar desde su cuenta.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Switcher row for accounts
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(classmates) { classmate ->
                                val isActive = classmate.id == (me?.id ?: 0)
                                Card(
                                    modifier = Modifier
                                        .clickable { viewModel.switchActiveUser(classmate.id) }
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = if (isActive) NeonCyan else CosmicSurfaceLighter,
                                            shape = RoundedCornerShape(20.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) ElectricViolet.copy(alpha = 0.25f) else CosmicBackground
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarImage(
                                            model = classmate.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = classmate.nickname,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) NeonCyan else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = CosmicSurfaceLighter, thickness = 1.dp)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tus Publicaciones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(CosmicSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, strokeBorderBrush(), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No has publicado nada aún",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    // Render personal posts card feed list
                    posts.forEach { localPost ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = me?.name ?: "Jeremy Jiménez",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (localPost.isVideo) "📹 Video" else "📷 Imagen",
                                        fontSize = 10.sp,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                AsyncImage(
                                    model = localPost.contentUri,
                                    contentDescription = "My Post Visual",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = localPost.caption,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Edit Profile details dialog
    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicSurfaceLighter),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Editar tu perfil",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        label = { Text("Nombre Completo") },
                        colors = transparentFieldColors()
                    )

                    OutlinedTextField(
                        value = editNickname,
                        onValueChange = { editNickname = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        label = { Text("Apodo / Nickname") },
                        colors = transparentFieldColors()
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(100.dp),
                        label = { Text("Tu Bio") },
                        colors = transparentFieldColors(),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Image input options
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = createTempImageUri()
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceLighter),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cámara", color = TextPrimary, fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceLighter),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = NeonPink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Galería", color = TextPrimary, fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = editAvatar,
                        onValueChange = { editAvatar = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        label = { Text("Enlace o ruta del Avatar") },
                        colors = transparentFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancelar", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                viewModel.updateMyProfile(editName, editNickname, editBio, editAvatar)
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Guardar", color = CosmicBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Account Registration Dialog
    if (showRegisterDialog) {
        var regName by remember { mutableStateOf("") }
        var regNickname by remember { mutableStateOf("") }
        var regBio by remember { mutableStateOf("") }
        var regAvatar by remember { mutableStateOf("") }
        var regPersonality by remember { mutableStateOf("") }
        var regHobby by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showRegisterDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicSurfaceLighter),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Registrar Nuevo Amigo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text("Nombre Completo (Ej: Juan Alberto)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = transparentFieldColors()
                    )

                    OutlinedTextField(
                        value = regNickname,
                        onValueChange = { regNickname = it },
                        label = { Text("Apodo (Ej: Juancho)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = transparentFieldColors()
                    )

                    OutlinedTextField(
                        value = regBio,
                        onValueChange = { regBio = it },
                        label = { Text("Biografía / Presentación") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(80.dp),
                        colors = transparentFieldColors(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = regAvatar,
                        onValueChange = { regAvatar = it },
                        label = { Text("URL de Avatar (Vacio para Aleatorio)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = transparentFieldColors()
                    )

                    OutlinedTextField(
                        value = regPersonality,
                        onValueChange = { regPersonality = it },
                        label = { Text("Personalidad (Para Chat con IA)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = transparentFieldColors()
                    )

                    OutlinedTextField(
                        value = regHobby,
                        onValueChange = { regHobby = it },
                        label = { Text("Hobby / Actividad Favorita") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = transparentFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showRegisterDialog = false }) {
                            Text("Cancelar", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                if (regName.isNotBlank() && regNickname.isNotBlank()) {
                                    viewModel.registerNewClassmate(
                                        name = regName,
                                        nickname = regNickname,
                                        bio = regBio,
                                        avatar = regAvatar,
                                        personality = regPersonality,
                                        hobby = regHobby
                                    )
                                    showRegisterDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            enabled = regName.isNotBlank() && regNickname.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Crear Perfil", color = CosmicBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NeonPink
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

// ==================== CLASSMATE PROFILE DETAIL ====================

@Composable
fun ClassmateProfileDialog(
    classmate: Classmate,
    viewModel: PromoViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            border = BorderStroke(1.dp, CosmicSurfaceLighter),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("classmate_profile_dialog_${classmate.id}")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar Circle
                Box {
                    AvatarImage(
                        model = classmate.avatarUrl,
                        contentDescription = classmate.name,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, NeonPink, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFF4ADE80), CircleShape)
                            .align(Alignment.BottomEnd)
                            .border(1.5.dp, CosmicSurface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = classmate.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Text(
                    text = "@${classmate.nickname}",
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceLighter),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Bio del Curso",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = classmate.bio,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Hobby Favorito", fontSize = 10.sp, color = TextSecondary)
                                Text(classmate.hobby, fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }

                            Column {
                                Text("Personalidad", fontSize = 10.sp, color = TextSecondary)
                                Text("Alegre / Genial", fontSize = 12.sp, color = NeonPink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            viewModel.selectChat(classmate.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chatear en privado", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helper styling extensions
@Composable
fun transparentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = CosmicSurfaceLighter,
    focusedContainerColor = CosmicBackground,
    unfocusedContainerColor = CosmicBackground
)

fun strokeBorderBrush(): Brush {
    return Brush.horizontalGradient(listOf(NeonCyan, NeonPink))
}
