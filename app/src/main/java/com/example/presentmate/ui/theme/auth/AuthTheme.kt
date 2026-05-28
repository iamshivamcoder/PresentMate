package com.example.presentmate.ui.theme.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.presentmate.ui.theme.Typography

// Prototype Dark Palette mapped to Material 3 Colors
val AuthDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC5C6CC),
    onPrimary = Color(0xFF111418), // Surface main text on primary btn
    primaryContainer = Color(0xFFC5C6CC),
    onPrimaryContainer = Color(0xFF111418),
    
    secondary = Color(0xFFBFC7D3),
    onSecondary = Color(0xFF29313A),
    secondaryContainer = Color(0xFFDBE3EF),
    onSecondaryContainer = Color(0xFF141C25),
    
    background = Color(0xFF111418), // surface-main
    onBackground = Color(0xFFFFFFFF), // text-primary
    
    surface = Color(0xFF111418),
    onSurface = Color(0xFFFFFFFF),
    
    surfaceVariant = Color(0xFF283039), // surface-accent
    onSurfaceVariant = Color(0xFF9DABB9), // text-secondary
    
    outline = Color(0xFF283039), // border-subtle
    outlineVariant = Color(0xFF45474A),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun AuthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuthDarkColorScheme,
        typography = Typography, // This already has Space Grotesk globally injected!
        content = content
    )
}
