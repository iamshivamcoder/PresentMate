package com.example.presentmate.ai

import android.graphics.Bitmap

/**
 * Platform-agnostic AI service interface.
 */
interface AIService {
    suspend fun sendMessage(message: String): AIResponse
    suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse
}

/**
 * Factory to create an AIService given saved preferences.
 */
object AIServiceFactory {
    fun create(
        platform: AIPlatform,
        apiKey: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 1024
    ): AIService? {
        if (apiKey.isBlank()) return null
        return when (platform) {
            AIPlatform.GEMINI -> GeminiService(apiKey)
            AIPlatform.OPENROUTER -> OpenRouterService(apiKey, temperature, maxTokens)
            AIPlatform.GROQ -> GroqService(apiKey, temperature, maxTokens)
            AIPlatform.COHERE -> CohereService(apiKey, temperature, maxTokens)
        }
    }
}
