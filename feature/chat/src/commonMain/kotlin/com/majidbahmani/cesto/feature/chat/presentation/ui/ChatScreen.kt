package com.majidbahmani.cesto.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.cesto.feature.chat.domain.model.AskFailure
import com.majidbahmani.cesto.feature.chat.presentation.model.ChatMessage
import com.majidbahmani.cesto.feature.chat.presentation.model.ThinkingStep
import com.majidbahmani.cesto.feature.chat.presentation.viewmodel.ChatUiState
import com.majidbahmani.cesto.feature.chat.presentation.viewmodel.ChatViewModel
import com.majidbahmani.cesto.feature.chat.resources.Res
import com.majidbahmani.cesto.feature.chat.resources.chat_based_on
import com.majidbahmani.cesto.feature.chat.resources.chat_error_busy
import com.majidbahmani.cesto.feature.chat.resources.chat_error_failed
import com.majidbahmani.cesto.feature.chat.resources.chat_error_key_rejected
import com.majidbahmani.cesto.feature.chat.resources.chat_error_no_connection
import com.majidbahmani.cesto.feature.chat.resources.chat_error_no_key
import com.majidbahmani.cesto.feature.chat.resources.chat_error_quota
import com.majidbahmani.cesto.feature.chat.resources.chat_error_too_many_steps
import com.majidbahmani.cesto.feature.chat.resources.chat_input_hint
import com.majidbahmani.cesto.feature.chat.resources.chat_intro
import com.majidbahmani.cesto.feature.chat.resources.chat_new
import com.majidbahmani.cesto.feature.chat.resources.chat_no_key_button
import com.majidbahmani.cesto.feature.chat.resources.chat_no_key_text
import com.majidbahmani.cesto.feature.chat.resources.chat_no_key_title
import com.majidbahmani.cesto.feature.chat.resources.chat_send
import com.majidbahmani.cesto.feature.chat.resources.chat_step_counting
import com.majidbahmani.cesto.feature.chat.resources.chat_step_products
import com.majidbahmani.cesto.feature.chat.resources.chat_step_ranking
import com.majidbahmani.cesto.feature.chat.resources.chat_step_receipts
import com.majidbahmani.cesto.feature.chat.resources.chat_step_spending
import com.majidbahmani.cesto.feature.chat.resources.chat_step_thinking
import com.majidbahmani.cesto.feature.chat.resources.chat_suggestion_spending
import com.majidbahmani.cesto.feature.chat.resources.chat_suggestion_top
import com.majidbahmani.cesto.feature.chat.resources.chat_suggestion_yogurt
import com.majidbahmani.cesto.feature.chat.resources.chat_title
import com.majidbahmani.cesto.feature.chat.resources.ic_send
import com.majidbahmani.cesto.systemdesign.component.CestoScreenTitle
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ChatRoute(
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onSuggestion = viewModel::onSuggestion,
        onNewChat = viewModel::onNewChat,
        onOpenSettings = onOpenSettings,
        contentPadding = contentPadding
    )
}

@Composable
internal fun ChatScreen(
    uiState: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** Space the floating bottom bar covers. */
    contentPadding: PaddingValues = PaddingValues()
) {
    // The input sits above the pill, or above the keyboard when it's open (it covers the pill).
    val bottom = maxOf(contentPadding.calculateBottomPadding(), WindowInsets.ime.asPaddingValues().calculateBottomPadding())
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(bottom = bottom)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) { CestoScreenTitle(stringResource(Res.string.chat_title)) }
                if (uiState.messages.isNotEmpty()) {
                    TextButton(onClick = onNewChat) { Text(stringResource(Res.string.chat_new)) }
                }
            }
            when (uiState.isReady) {
                null -> Unit

                // key store not read yet: a blink, no spinner
                false -> NoKey(onOpenSettings, Modifier.padding(horizontal = 24.dp))

                true -> {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (uiState.messages.isEmpty()) {
                            Suggestions(onSuggestion)
                        } else {
                            Messages(uiState, onOpenSettings)
                        }
                    }
                    InputRow(uiState, onInputChange, onSend)
                }
            }
        }
    }
}

@Composable
private fun Suggestions(onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.chat_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        listOf(Res.string.chat_suggestion_spending, Res.string.chat_suggestion_yogurt, Res.string.chat_suggestion_top).forEach {
            val text = stringResource(it)
            SuggestionChip(onClick = { onSuggestion(text) }, label = { Text(text) })
        }
    }
}

@Composable
private fun Messages(uiState: ChatUiState, onOpenSettings: () -> Unit) {
    val listState = rememberLazyListState()
    // Follow the conversation: the newest message, or the "thinking" line under it.
    val lastIndex = uiState.messages.size + (if (uiState.thinking != null) 1 else 0) - 1
    LaunchedEffect(lastIndex) { if (lastIndex >= 0) listState.animateScrollToItem(lastIndex) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.messages, key = { it.id }) { message ->
            when (message) {
                is ChatMessage.Question -> QuestionBubble(message.text)
                is ChatMessage.Answer -> AnswerBubble(message)
                is ChatMessage.Failure -> FailureBubble(message.reason, onOpenSettings)
            }
        }
        uiState.thinking?.let { step -> item(key = "thinking") { Thinking(step) } }
    }
}

@Composable
private fun QuestionBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun AnswerBubble(answer: ChatMessage.Answer) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
        modifier = Modifier.widthIn(max = 340.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SelectionContainer { Text(answer.text, style = MaterialTheme.typography.bodyLarge) }
            if (answer.receiptCount > 0) {
                Text(
                    text = pluralStringResource(Res.plurals.chat_based_on, answer.receiptCount, answer.receiptCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FailureBubble(reason: AskFailure, onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
        modifier = Modifier.widthIn(max = 340.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(stringResource(reason.message()), style = MaterialTheme.typography.bodyMedium)
            if (reason == AskFailure.NO_KEY || reason == AskFailure.KEY_REJECTED) {
                TextButton(onClick = onOpenSettings) { Text(stringResource(Res.string.chat_no_key_button)) }
            }
        }
    }
}

@Composable
private fun Thinking(step: ThinkingStep) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(step.label()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InputRow(uiState: ChatUiState, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = uiState.input,
            onValueChange = onInputChange,
            placeholder = { Text(stringResource(Res.string.chat_input_hint)) },
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (uiState.canSend) onSend() }),
            modifier = Modifier.weight(1f)
        )
        FilledIconButton(onClick = onSend, enabled = uiState.canSend, modifier = Modifier.size(48.dp)) {
            Icon(
                painterResource(Res.drawable.ic_send),
                contentDescription = stringResource(Res.string.chat_send),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NoKey(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.chat_no_key_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(Res.string.chat_no_key_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onOpenSettings) { Text(stringResource(Res.string.chat_no_key_button)) }
        }
    }
}

private fun ThinkingStep.label(): StringResource = when (this) {
    ThinkingStep.THINKING -> Res.string.chat_step_thinking
    ThinkingStep.FINDING_PRODUCTS -> Res.string.chat_step_products
    ThinkingStep.COUNTING -> Res.string.chat_step_counting
    ThinkingStep.ADDING_SPENDING -> Res.string.chat_step_spending
    ThinkingStep.RANKING -> Res.string.chat_step_ranking
    ThinkingStep.LISTING_RECEIPTS -> Res.string.chat_step_receipts
}

private fun AskFailure.message(): StringResource = when (this) {
    AskFailure.NO_KEY -> Res.string.chat_error_no_key
    AskFailure.KEY_REJECTED -> Res.string.chat_error_key_rejected
    AskFailure.QUOTA -> Res.string.chat_error_quota
    AskFailure.BUSY -> Res.string.chat_error_busy
    AskFailure.NO_CONNECTION -> Res.string.chat_error_no_connection
    AskFailure.TOO_MANY_STEPS -> Res.string.chat_error_too_many_steps
    AskFailure.FAILED -> Res.string.chat_error_failed
}

private val previewMessages = listOf(
    ChatMessage.Question(0, "How many yogurts did I buy in September?"),
    ChatMessage.Answer(1, "You bought 24 yogurts in September: 5 packs of Iogurte grego natural 4x125 g and 4 single ones.", 6),
    ChatMessage.Question(2, "And in August?"),
    ChatMessage.Failure(3, AskFailure.QUOTA)
)

@Preview
@Composable
private fun ChatConversationPreview() {
    CestoTheme {
        ChatScreen(
            uiState = ChatUiState(isReady = true, messages = previewMessages, thinking = ThinkingStep.COUNTING),
            onInputChange = {},
            onSend = {},
            onSuggestion = {},
            onNewChat = {},
            onOpenSettings = {}
        )
    }
}

@Preview
@Composable
private fun ChatEmptyDarkPreview() {
    CestoTheme(darkTheme = true) {
        ChatScreen(uiState = ChatUiState(isReady = true), onInputChange = {
        }, onSend = {}, onSuggestion = {}, onNewChat = {}, onOpenSettings = {})
    }
}

@Preview
@Composable
private fun ChatNoKeyPreview() {
    CestoTheme {
        ChatScreen(uiState = ChatUiState(isReady = false), onInputChange = {
        }, onSend = {}, onSuggestion = {}, onNewChat = {}, onOpenSettings = {})
    }
}
