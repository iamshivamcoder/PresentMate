package com.example.presentmate.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.presentmate.ai.AIPlatform
import com.example.presentmate.ai.AIPreferences
import kotlinx.coroutines.launch

@Composable
fun AIPreferencesScreen() {
    val context = LocalContext.current

    var selectedPlatform by remember { mutableStateOf(AIPreferences.getPlatform(context)) }
    // Each platform's key is loaded independently
    var apiKey by remember { mutableStateOf(AIPreferences.getApiKeyFor(context, selectedPlatform)) }
    var temperature by remember { mutableFloatStateOf(AIPreferences.getTemperature(context)) }
    var maxTokens by remember { mutableIntStateOf(AIPreferences.getMaxTokens(context)) }
    var showKey by remember { mutableStateOf(false) }
    // Fix #15 — key test state: null=untested, true=valid, false=invalid
    var keyTestState by remember { mutableStateOf<Boolean?>(null) }
    var keyTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Section header ────────────────────────────────────────────────────
        SectionLabel("Choose AI Provider")

        // ── Platform cards ────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AIPlatform.entries.forEach { platform ->
                val hasSavedKey = AIPreferences.hasKeyFor(context, platform)
                PlatformCard(
                    platform = platform,
                    isSelected = platform == selectedPlatform,
                    hasSavedKey = hasSavedKey,
                    onClick = {
                        selectedPlatform = platform
                        AIPreferences.setPlatform(context, platform)
                        // Load this provider's own key into the field
                        apiKey = AIPreferences.getApiKeyFor(context, platform)
                        showKey = false
                    }
                )
            }
        }

        // ── API Key section ───────────────────────────────────────────────────
        SectionLabel("API Key — ${selectedPlatform.displayName}")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Key field
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Paste your ${selectedPlatform.displayName} key") },
                    visualTransformation = if (showKey) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                // Get key link
                val ctx = LocalContext.current
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedPlatform.apiKeyUrl))
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Get your free key →",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Save + Test buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Test Key button (Fix #15)
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            if (apiKey.isBlank()) {
                                Toast.makeText(context, "Enter a key first", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            keyTesting = true
                            keyTestState = null
                            scope.launch {
                                keyTestState = try {
                                    val svc = com.example.presentmate.ai.AIServiceFactory.create(
                                        platform = selectedPlatform,
                                        apiKey = apiKey.trim()
                                    )
                                    if (svc == null) false else {
                                        val result = svc.sendMessage("hi")
                                        result is com.example.presentmate.ai.AIResponse.Success
                                    }
                                } catch (e: Exception) { false }
                                keyTesting = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !keyTesting
                    ) {
                        if (keyTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Test Key")
                        }
                    }

                    Button(
                        onClick = {
                            AIPreferences.setApiKeyFor(context, selectedPlatform, apiKey.trim())
                            keyTestState = null
                            Toast.makeText(
                                context,
                                "✓ ${selectedPlatform.displayName} key saved",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Inline key test result
                keyTestState?.let { valid ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (valid) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (valid) Color(0xFF4CAF50)
                                   else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (valid) "Key is valid ✓" else "Key rejected — check and retry",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (valid) Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ── Model settings ────────────────────────────────────────────────────
        SectionLabel("Model Settings")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Temperature
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Creativity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Text(
                            "Lower = more focused  •  Higher = more creative",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "%.1f".format(temperature),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    onValueChangeFinished = { AIPreferences.setTemperature(context, temperature) },
                    valueRange = 0f..1f
                )

                // Max tokens
                Spacer(Modifier.height(4.dp))
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

        Spacer(Modifier.height(8.dp))
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun PlatformCard(
    platform: AIPlatform,
    isSelected: Boolean,
    hasSavedKey: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "platform_bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Platform icon circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + description
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        platform.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (hasSavedKey) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Key saved",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    platform.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (platform.supportsImages)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (platform.supportsImages) "Text + Images" else "Text only",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (platform.supportsImages) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Selected indicator
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
