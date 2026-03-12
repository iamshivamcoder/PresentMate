package com.example.presentmate.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class AIPlatform(
    val displayName: String,
    val description: String,
    val apiKeyUrl: String,
    val supportsImages: Boolean
) {
    GEMINI(
        displayName = "Google Gemini",
        description = "Powerful multimodal AI by Google. Free tier available.",
        apiKeyUrl = "https://aistudio.google.com/app/apikey",
        supportsImages = true
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        description = "Access 100+ models including free LLaMA, Mistral & more.",
        apiKeyUrl = "https://openrouter.ai/keys",
        supportsImages = false
    ),
    GROQ(
        displayName = "Groq",
        description = "Ultra-fast inference — free tier with LLaMA 3 & Mixtral.",
        apiKeyUrl = "https://console.groq.com/keys",
        supportsImages = false
    ),
    COHERE(
        displayName = "Cohere",
        description = "Free Command-R model for chat and reasoning tasks.",
        apiKeyUrl = "https://dashboard.cohere.com/api-keys",
        supportsImages = false
    )
}

object AIPreferences {
    private const val PREFS_NAME = "ai_preferences"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_PLATFORM = "ai_platform"
    private const val KEY_TEMPERATURE = "temperature"
    private const val KEY_MAX_TOKENS = "max_tokens"
    private const val KEY_SYSTEM_PROMPT_SUFFIX = "system_prompt_suffix"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, key: String) =
        prefs(context).edit { putString(KEY_API_KEY, key) }

    fun getPlatform(context: Context): AIPlatform {
        val name = prefs(context).getString(KEY_PLATFORM, AIPlatform.GEMINI.name)
        return AIPlatform.entries.find { it.name == name } ?: AIPlatform.GEMINI
    }

    fun setPlatform(context: Context, platform: AIPlatform) =
        prefs(context).edit { putString(KEY_PLATFORM, platform.name) }

    fun getTemperature(context: Context): Float =
        prefs(context).getFloat(KEY_TEMPERATURE, 0.7f)

    fun setTemperature(context: Context, value: Float) =
        prefs(context).edit { putFloat(KEY_TEMPERATURE, value) }

    fun getMaxTokens(context: Context): Int =
        prefs(context).getInt(KEY_MAX_TOKENS, 1024)

    fun setMaxTokens(context: Context, value: Int) =
        prefs(context).edit { putInt(KEY_MAX_TOKENS, value) }

    fun hasValidKey(context: Context): Boolean =
        getApiKey(context).isNotBlank()
}
