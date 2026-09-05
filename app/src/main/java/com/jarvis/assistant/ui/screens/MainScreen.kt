package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.model.AssistantState
import com.jarvis.assistant.model.Message
import com.jarvis.assistant.ui.components.AICore
import com.jarvis.assistant.ui.components.WaveformVisualizer
import com.jarvis.assistant.ui.theme.JarvisCyan
import com.jarvis.assistant.ui.theme.JarvisTextDim
import com.jarvis.assistant.viewmodel.AssistantViewModel

@Composable
fun MainScreen(
    viewModel: AssistantViewModel,
    onOpenSettings: () -> Unit,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "JARVIS",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisCyan,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                IconButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = JarvisTextDim)
                }
            }

            // Conversation history
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message -> MessageBubble(message) }
            }

            // Status text
            Text(
                text = statusText(state),
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextDim,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (transcript.isNotBlank() && state != AssistantState.IDLE) {
                Text(
                    text = "\"$transcript\"",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            WaveformVisualizer(active = state == AssistantState.LISTENING || state == AssistantState.SPEAKING)

            // AI core + mic button
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AICore(state = state)
                IconButton(
                    onClick = {
                        if (!hasMicPermission) {
                            onRequestMicPermission()
                        } else if (state == AssistantState.LISTENING) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Listen",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        pendingConfirmation?.let { pending ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelPendingCommand() },
                title = { Text("Confirm action") },
                text = { Text(pending.confirmationPrompt) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmPendingCommand() }) { Text("Yes") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelPendingCommand() }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == Message.Role.USER
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (isUser) JarvisCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun statusText(state: AssistantState): String = when (state) {
    AssistantState.IDLE -> "Tap the mic or say \"Hey Jarvis\""
    AssistantState.LISTENING -> "Listening…"
    AssistantState.THINKING -> "Thinking…"
    AssistantState.SPEAKING -> "Speaking…"
    AssistantState.ERROR -> "Something went wrong — tap the mic to try again"
}
