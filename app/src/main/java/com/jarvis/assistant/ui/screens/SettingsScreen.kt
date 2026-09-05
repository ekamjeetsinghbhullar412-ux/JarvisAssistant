package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.theme.JarvisCyan
import com.jarvis.assistant.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Assistant name", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = settings.assistantName,
            onValueChange = { viewModel.updateAssistantName(it) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Text("Speech speed: ${"%.1f".format(settings.speechRate)}x", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = settings.speechRate,
            onValueChange = { viewModel.updateSpeechRate(it) },
            valueRange = 0.5f..2f,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Wake word (\"Hey Jarvis\")", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Requires the app to be open / foreground service running — Android does not allow always-on mic listening while fully locked.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(checked = settings.wakeWordEnabled, onCheckedChange = { viewModel.updateWakeWordEnabled(it) })
        }

        Text("Animation intensity: ${"%.1f".format(settings.animationIntensity)}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = settings.animationIntensity,
            onValueChange = { viewModel.updateAnimationIntensity(it) },
            valueRange = 0f..2f,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
        )

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            "Jarvis only listens when you tap the mic or (if enabled) say the wake phrase while the app is active. " +
                "Conversation history is stored only on this device.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        TextButton(onClick = { viewModel.clearConversationHistory() }) {
            Text("Clear conversation history")
        }
        TextButton(onClick = { viewModel.resetAllSettings() }) {
            Text("Reset all settings")
        }
    }
}
