package com.example.presentmate.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.presentmate.ai.AIPreferences
import com.example.presentmate.viewmodel.AIAssistantViewModel
import com.example.presentmate.viewmodel.ChatMessage
import com.example.presentmate.viewmodel.ConfirmationState

@Composable
fun AIAssistantScreen(viewModel: AIAssistantViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val platform = remember { AIPreferences.getPlatform(context) }


    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val listState = rememberLazyListState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAiServiceState()
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Confirmation dialogs
    when (val state = uiState.confirmationState) {
        is ConfirmationState.FirstConfirmation -> {
            ConfirmationDialog(
                title = "Add Records?",
                message = "Found ${state.records.size} attendance record(s). Add them to your database?",
                confirmText = "Yes, Continue",
                onConfirm = { viewModel.onFirstConfirmation() },
                onDismiss = { viewModel.onCancelConfirmation() }
            )
        }
        is ConfirmationState.SecondConfirmation -> {
            ConfirmationDialog(
                title = "Final Confirmation",
                message = "This will add ${state.records.size} record(s) to your attendance database. This action cannot be undone.",
                confirmText = "Add Records",
                onConfirm = { viewModel.onSecondConfirmation() },
                onDismiss = { viewModel.onCancelConfirmation() }
            )
        }
        ConfirmationState.None -> { /* no dialog */ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ── API key missing banner (compact) ──────────────────────────────────
            if (uiState.apiKeyMissing) {
                ApiKeyBanner(platformName = platform.displayName)
            }

            // ── Message list ──────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty() && !uiState.apiKeyMissing) {
                    EmptyState(platformName = platform.displayName)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(uiState.messages, key = { _, message -> message.id }) { index, message ->
                        if (index == 0 && !message.isFromUser && message.content.startsWith("👋")) {
                            WelcomeHeroCard(
                                platformName = platform.displayName,
                                onSuggestionClick = { suggestion ->
                                    inputText = suggestion
                                }
                            )
                        } else {
                            ChatMessageBubble(message = message)
                        }
                    }
                }
            }

            // ── Image preview strip ───────────────────────────────────────────────
            selectedImageUri?.let { uri ->
                val previewBitmap = remember(uri) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }
                ImagePreviewStrip(bitmap = previewBitmap, onRemove = { selectedImageUri = null })
            }

            // ── Input bar ─────────────────────────────────────────────────────────
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                enabled = !uiState.apiKeyMissing,
                supportsImage = platform.supportsImages,
                hasImage = selectedImageUri != null,
                onAttachImage = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSend = {
                    if (inputText.isNotBlank() || selectedImageUri != null) {
                        val uri = selectedImageUri
                        if (uri != null) {
                            viewModel.sendMessageWithImage(
                                text = inputText,
                                imageUri = uri,
                                resolveBitmap = { u ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        ImageDecoder.decodeBitmap(
                                            ImageDecoder.createSource(context.contentResolver, u)
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaStore.Images.Media.getBitmap(context.contentResolver, u)
                                    }
                                }
                            )
                            selectedImageUri = null
                        } else {
                            viewModel.sendMessage(inputText)
                        }
                        inputText = ""
                    }
                }
            )
        }
    }
}

// ── Welcome Hero Card with suggestions ───────────────────────────────────────

data class SuggestionItem(
    val title: String,
    val desc: String,
    val prompt: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun WelcomeHeroCard(
    platformName: String,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surface
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PresentMate AI",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Powered by $platformName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your intelligent assistant for managing schedules, analyzing study patterns, and auto-logging attendance sheets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Quick suggestions:",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val suggestions = listOf(
            SuggestionItem(
                title = "Analyze Class Log",
                desc = "Extract data from a sheet",
                prompt = "Please analyze this attendance sheet",
                icon = Icons.Default.Image
            ),
            SuggestionItem(
                title = "Sync Google Calendar",
                desc = "Integrate your classes",
                prompt = "How do I sync my classes with Google Calendar?",
                icon = Icons.Default.CalendarMonth
            ),
            SuggestionItem(
                title = "Attendance Status",
                desc = "Check overall summary",
                prompt = "What is my attendance status?",
                icon = Icons.Default.Analytics
            ),
            SuggestionItem(
                title = "Study Schedule Tips",
                desc = "Optimize study routine",
                prompt = "Can you give me some tips on creating a productive study schedule?",
                icon = Icons.Default.AutoStories
            )
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.chunked(2).forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { suggestion ->
                        SuggestionCard(
                            item = suggestion,
                            onClick = { onSuggestionClick(suggestion.prompt) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    item: SuggestionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = item.desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    enabled: Boolean,
    supportsImage: Boolean,
    hasImage: Boolean,
    onAttachImage: () -> Unit,
    onSend: () -> Unit
) {
    val canSend = (text.isNotBlank() || hasImage) && enabled
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = if (canSend) {
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attach image button
            if (supportsImage) {
                IconButton(
                    onClick = onAttachImage,
                    enabled = enabled,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasImage) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Attach image",
                        tint = if (hasImage) MaterialTheme.colorScheme.onPrimaryContainer
                               else if (enabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            // Input field directly placed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        "Ask AI Assistant...",
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 15.sp
                        )
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
            }

            Spacer(Modifier.width(6.dp))

            // Send button
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        } else {
                            SolidColor(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        }
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.isFromUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI avatar
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "AI",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        // Bubble
        Column(
            modifier = Modifier.widthIn(max = 290.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (message.imageUri != null || message.content.isNotBlank()) {
                val bubbleShape = if (isUser) {
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 4.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                }

                if (isUser) {
                    Box(
                        modifier = Modifier
                            .shadow(elevation = 2.dp, shape = bubbleShape)
                            .clip(bubbleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            message.imageUri?.let { uri ->
                                val ctx = LocalContext.current
                                val bitmap = remember(uri) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
                                    }
                                }
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Attached image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            if (message.content.isNotBlank()) {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                } else {
                    // AI bubble
                    Surface(
                        shape = bubbleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        ),
                        tonalElevation = 0.dp,
                        modifier = Modifier.shadow(elevation = 1.dp, shape = bubbleShape)
                    ) {
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            // Left indicator line
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                            )
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                if (message.isLoading) {
                                    TypingIndicator()
                                } else if (message.content.isNotBlank()) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isUser) Spacer(Modifier.width(36.dp))
    }
}

// ── Animated 3-dot typing indicator ──────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotCount = 3
    val delays = listOf(0, 160, 320)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        repeat(dotCount) { i ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0f at delays[i] using LinearEasing
                        -6f at delays[i] + 150 using LinearEasing
                        0f at delays[i] + 300 using LinearEasing
                        0f at 900
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            )
        }
    }
}

// ── Image preview strip ───────────────────────────────────────────────────────

@Composable
private fun ImagePreviewStrip(bitmap: Bitmap, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Image attached", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Will be sent with your message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(platformName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                "AI Assistant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Powered by $platformName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ask me about your attendance, share an attendance sheet image, or let me help you add records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── API key missing compact banner ────────────────────────────────────────────

@Composable
private fun ApiKeyBanner(platformName: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "No $platformName key set — go to Settings → Preferences → AI Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Confirmation dialog ───────────────────────────────────────────────────────

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
