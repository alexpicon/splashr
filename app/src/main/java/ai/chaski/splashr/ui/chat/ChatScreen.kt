package ai.chaski.splashr.ui.chat

import ai.chaski.splashr.ai.AiSuggestionService
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.repository.PhotoRepository
import ai.chaski.splashr.ui.AppViewModelProvider
import ai.chaski.splashr.ui.components.SplashrAsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One entry in the chat search transcript. */
sealed interface ChatMessage {
    data class User(val text: String) : ChatMessage
    data class Assistant(val interpretation: String, val results: List<Photo>) : ChatMessage
}

class ChatViewModel(
    private val repository: PhotoRepository,
    private val aiService: AiSuggestionService,
) : ViewModel() {

    private val rawMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage.Assistant(
                interpretation = "Hi! Tell me what you'd like to see. Try \"dark city photos\", " +
                    "\"photos by Alex\", or \"vertical travel photos\".",
                results = emptyList(),
            ),
        ),
    )

    /** Transcript with live "saved" state folded into every result photo. */
    val messages: StateFlow<List<ChatMessage>> =
        combine(rawMessages, repository.observeSavedIds()) { messages, savedIds ->
            messages.map { message ->
                if (message is ChatMessage.Assistant) {
                    message.copy(
                        results = message.results.map { it.copy(isSaved = it.id in savedIds) },
                    )
                } else {
                    message
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    val examples = listOf(
        "Show me dark city photos",
        "Find nature images",
        "Photos by Alex",
        "Vertical travel photos",
    )

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isThinking.value) return
        rawMessages.update { it + ChatMessage.User(trimmed) }
        viewModelScope.launch {
            _isThinking.value = true
            val parsed = aiService.parseChatQuery(trimmed)
            val results = repository.search(parsed.filter)
            rawMessages.update {
                it + ChatMessage.Assistant(parsed.interpretation, results)
            }
            _isThinking.value = false
        }
    }

    fun toggleSave(photo: Photo) {
        viewModelScope.launch { repository.toggleSaved(photo) }
    }
}

@Composable
fun ChatScreen(
    onPhotoClick: (String) -> Unit,
    viewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        val lastIndex = (messages.size - 1 + if (isThinking) 1 else 0).coerceAtLeast(0)
        listState.animateScrollToItem(lastIndex)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Chat search") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message, onPhotoClick = onPhotoClick)
                }
                if (isThinking) {
                    item { ThinkingBubble() }
                }
            }
            ExampleChips(
                examples = viewModel.examples,
                onPick = { viewModel.send(it) },
            )
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    viewModel.send(input)
                    input = ""
                },
            )
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage, onPhotoClick: (String) -> Unit) {
    when (message) {
        is ChatMessage.User -> Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Text(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }

        is ChatMessage.Assistant -> Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = message.interpretation,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (message.results.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(message.results, key = { it.id }) { photo ->
                            ChatResultCard(photo = photo, onClick = { onPhotoClick(photo.id) })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${message.results.size} photos found",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatResultCard(photo: Photo, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.size(width = 132.dp, height = 168.dp),
    ) {
        SplashrAsyncImage(photo.imageUrl, photo.title, Modifier.fillMaxSize())
    }
}

@Composable
private fun ThinkingBubble() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Interpreting your request…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExampleChips(examples: List<String>, onPick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(examples) { example ->
            AssistChip(
                onClick = { onPick(example) },
                label = {
                    Text(example, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Describe what you want to see") },
            shape = RoundedCornerShape(24.dp),
            maxLines = 3,
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
