package com.example.presentmate.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Email // Placeholder
import androidx.compose.material.icons.filled.Person // Placeholder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AboutDeveloperScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "About the Developer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Developer Photo
        Image(
            painter = ColorPainter(MaterialTheme.colorScheme.secondary), // Placeholder
            contentDescription = "Developer Photo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // Name & Tagline
        Text(text = "Shivam", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "Solo Developer & Creator of Present Mate",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic
        )

        // Intro Text
        Text(
            text = "Hello! I'm the single person behind the design, code, and support for this app. I'm passionate about building simple, beautiful, and effective tools that solve real-world problems.\n\nAs a solo developer, your feedback isn't just a support ticket—it's a direct conversation that shapes the future of this app. Thank you for being a part of this journey.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Mission Section
        SectionCard(
            title = "My Mission",
            content = "To create high-quality, privacy-focused apps that respect the user. I believe in building tools without a large corporate overhead, focusing only on the features that truly matter to you."
        )

        // Other Work Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("My Other Work", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("I'm always building and experimenting. You can check out my other projects, articles, or open-source contributions on my portfolio website.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { /* TODO */ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Visit My Portfolio")
                }
            }
        }

        // Connect With Me
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Connect With Me", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { /* TODO: LinkedIn URL */ }) {
                    Icon(Icons.Default.Person, contentDescription = "LinkedIn", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { /* TODO: X/Twitter URL */ }) {
                    Icon(Icons.Default.Person, contentDescription = "X / Twitter", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { /* TODO: GitHub URL */ }) {
                    Icon(Icons.Default.Email, contentDescription = "GitHub", modifier = Modifier.size(32.dp)) // Placeholder icon
                }
            }
        }

        // Support Button
        Button(onClick = { /* TODO */ }) {
            Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Buy Me a Coffee")
        }
        Text(
            text = "If you find this app helpful, consider supporting its development!",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = content)
        }
    }
}
