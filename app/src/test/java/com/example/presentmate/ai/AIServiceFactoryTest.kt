package com.example.presentmate.ai

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIServiceFactoryTest {

    @Test
    fun create_withBlankApiKey_returnsNull() {
        val result = AIServiceFactory.create(AIPlatform.GEMINI, "", 0.7f, 1000)
        assertNull(result)
        
        val resultSpaces = AIServiceFactory.create(AIPlatform.GEMINI, "   ", 0.7f, 1000)
        assertNull(resultSpaces)
    }

    @Test
    fun create_withGemini_returnsGeminiService() {
        val result = AIServiceFactory.create(AIPlatform.GEMINI, "test_key", 0.7f, 1000)
        assertTrue(result is GeminiService)
    }

    @Test
    fun create_withOpenRouter_returnsOpenRouterService() {
        val result = AIServiceFactory.create(AIPlatform.OPENROUTER, "test_key", 0.7f, 1000)
        assertTrue(result is OpenRouterService)
    }

    @Test
    fun create_withGroq_returnsGroqService() {
        val result = AIServiceFactory.create(AIPlatform.GROQ, "test_key", 0.7f, 1000)
        assertTrue(result is GroqService)
    }

    @Test
    fun create_withCohere_returnsCohereService() {
        val result = AIServiceFactory.create(AIPlatform.COHERE, "test_key", 0.7f, 1000)
        assertTrue(result is CohereService)
    }
}
