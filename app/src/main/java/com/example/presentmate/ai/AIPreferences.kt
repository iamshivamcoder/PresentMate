package com.example.presentmate.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
    private const val TAG = "AIPreferences"

    // Sensitive prefs — API keys stored in EncryptedSharedPreferences
    private const val SECURE_PREFS_NAME = "ai_secure_preferences"
    // Non-sensitive prefs — platform choice, model settings
    private const val PREFS_NAME = "ai_preferences"
    private const val KEY_PLATFORM = "ai_platform"
    private const val KEY_TEMPERATURE = "temperature"
    private const val KEY_MAX_TOKENS = "max_tokens"

    // Per-provider key prefix: "api_key_GEMINI", "api_key_GROQ", etc.
    private fun platformKeySlot(platform: AIPlatform) = "api_key_${platform.name}"

    // ── Encrypted prefs for API keys ─────────────────────────────────────────

    private fun securePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for devices that can't init the Keystore (extremely rare)
            Log.e(TAG, "EncryptedSharedPreferences unavailable, falling back to plain prefs", e)
            context.getSharedPreferences(SECURE_PREFS_NAME + "_plain", Context.MODE_PRIVATE)
        }
    }

    private fun plainPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Per-provider key access ───────────────────────────────────────────────

    /** Get the saved API key for a specific provider (decrypted from secure storage). */
    fun getApiKeyFor(context: Context, platform: AIPlatform): String =
        securePrefs(context).getString(platformKeySlot(platform), "") ?: ""

    /** Save an API key for a specific provider (encrypted in secure storage). */
    fun setApiKeyFor(context: Context, platform: AIPlatform, key: String) =
        securePrefs(context).edit { putString(platformKeySlot(platform), key) }

    /** Returns true if the given provider has a non-blank key saved. */
    fun hasKeyFor(context: Context, platform: AIPlatform): Boolean =
        getApiKeyFor(context, platform).isNotBlank()

    // ── Convenience: active platform's key (used by AIServiceFactory) ─────────

    /** Get the API key for the currently active platform. */
    fun getApiKey(context: Context): String =
        getApiKeyFor(context, getPlatform(context))

    /** Save a key for the currently active platform. */
    fun setApiKey(context: Context, key: String) =
        setApiKeyFor(context, getPlatform(context), key)

    /** True if the active platform has a valid key. */
    fun hasValidKey(context: Context): Boolean =
        getApiKey(context).isNotBlank()

    // ── Platform (non-sensitive — plain prefs) ────────────────────────────────

    fun getPlatform(context: Context): AIPlatform {
        val name = plainPrefs(context).getString(KEY_PLATFORM, AIPlatform.GEMINI.name)
        return AIPlatform.entries.find { it.name == name } ?: AIPlatform.GEMINI
    }

    fun setPlatform(context: Context, platform: AIPlatform) =
        plainPrefs(context).edit { putString(KEY_PLATFORM, platform.name) }

    // ── Model settings (non-sensitive — plain prefs) ──────────────────────────

    fun getTemperature(context: Context): Float =
        plainPrefs(context).getFloat(KEY_TEMPERATURE, 0.7f)

    fun setTemperature(context: Context, value: Float) =
        plainPrefs(context).edit { putFloat(KEY_TEMPERATURE, value) }

    fun getMaxTokens(context: Context): Int =
        plainPrefs(context).getInt(KEY_MAX_TOKENS, 1024)

    fun setMaxTokens(context: Context, value: Int) =
        plainPrefs(context).edit { putInt(KEY_MAX_TOKENS, value) }
}
