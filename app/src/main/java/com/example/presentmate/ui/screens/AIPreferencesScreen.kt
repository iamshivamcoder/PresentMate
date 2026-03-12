package com.example.presentmate.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.presentmate.ai.AIPreferences
import com.example.presentmate.ai.AIPlatform

@Composable
fun AIPreferencesScreen() {
    val context = LocalContext.current

    var selectedPlatform by remember { mutableStateOf(AIPreferences.getPlatform(context)) }
    var apiKey by remember { mutableStateOf(AIPreferences.getApiKey(context)) }
    var temperature by remember { mutableFloatStateOf(AIPreferences.getTemperature(context)) }
    var maxTokens by remember { mutableIntStateOf(AIPreferences.getMaxTokens(context)) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Platform Selector
        SettingsGroup("AI Platform") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AIPlatform.entries.forEach { platform ->
                    PlatformCard(
                        platform = platform,
                        isSelected = platform == selectedPlatform,
                        onClick = {
                            selectedPlatform = platform
                            AIPreferences.setPlatform(context, platform)
                            // Clear key when switching platforms
                            apiKey = AIPreferences.getApiKey(context).let {
                                if (AIPreferences.getPlatform(context) == platform) it else ""
                            }
                        }
                    )
                }
            }
        }

        // API Key
        SettingsGroup("API Key") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            "${selectedPlatform.displayName} API Key",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Paste your API key here") },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            androidx.compose.material3.TextButton(onClick = { showKey = !showKey }) {
                                Text(if (showKey) "Hide" else "Show")
                            }
                        },
                        singleLine = true
                    )
                    Text(
                        "Get your key at: ${selectedPlatform.apiKeyUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = {
                            AIPreferences.setApiKey(context, apiKey.trim())
                            Toast.makeText(context, "API key saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Save API Key")
                    }
                }
            }
        }

        // Temperature
        SettingsGroup("Model Settings") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Creativity / Temperature", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        "Lower = more focused, Higher = more creative",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            onValueChangeFinished = { AIPreferences.setTemperature(context, temperature) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            "%.1f".format(temperature),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Max Response Length", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    val tokenOptions = listOf(512, 1024, 2048, 4096)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tokenOptions.forEach { tokens ->
                            FilterChip(
                                selected = maxTokens == tokens,
                                onClick = {
                                    maxTokens = tokens
                                    AIPreferences.setMaxTokens(context, tokens)
                                },
                                label = { Text(if (tokens >= 1024) "${tokens / 1024}K" else "$tokens") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCard(
    platform: AIPlatform,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    platform.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    platform.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!platform.supportsImages) {
                    Text(
                        "Text only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        "Text + Images",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
