package space.suiten.core.service

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import space.suiten.BuildConfig
import space.suiten.domain.model.AIResult
import space.suiten.domain.model.NextAction
import java.util.Date
import java.util.UUID

/**
 * AI Service for processing captures with Gemini AI
 *
 * PRIVACY GUARDRAILS:
 * - OCR text is NEVER stored or logged - only passed temporarily for processing
 * - Every output is checked by [AIGuardrails]; output that breaks a rule is regenerated
 *   once with feedback, and replaced with a reader-only fallback if it still breaks one
 * - Prompts follow SuiteN's belief that the answer is in the reader's head (see [AIPrompts])
 */
class AIService(
    private val generate: (suspend (String) -> String)? = null
) {

    private val gson = Gson()

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.9f
                maxOutputTokens = 1024
            }
        )
    }

    private val chatModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.8f
                topP = 0.9f
                maxOutputTokens = 512
            }
        )
    }

    companion object {
        /** First attempt plus one regeneration with feedback. */
        private const val MAX_ATTEMPTS = 2
        private const val MAX_NEXT_ACTIONS = 3
    }

    data class AIResponse(
        @SerializedName("draft_summary") val draftSummary: String = "",
        @SerializedName("deepening_question") val deepeningQuestion: String = "",
        @SerializedName("next_actions") val nextActions: List<NextActionDto> = emptyList(),
        @SerializedName("topic_tags") val topicTags: List<String> = emptyList()
    )

    data class NextActionDto(
        val type: String = "dialog",
        val title: String = "",
        val description: String = "",
        val query: String? = null,
        val link: String? = null
    )

    private val hasModel: Boolean
        get() = generate != null || BuildConfig.GEMINI_API_KEY.isNotEmpty()

    private suspend fun callModel(prompt: String, chat: Boolean = false): String {
        generate?.let { return it(prompt) }
        val model = if (chat) chatModel else generativeModel
        return model.generateContent(prompt).text ?: throw IllegalStateException("Empty response from Gemini")
    }

    /**
     * Process capture with Gemini AI
     *
     * @param ocrText MEMORY ONLY - will not be stored. Can be null if OCR failed or no image.
     * @param userMemo User's memo
     * @param emotionLabels Labels of the capture's emotion stamps, e.g. "疑問・共感"
     * @param pastReactions Earlier reactions in the same book (AI output, never book text)
     * @return Pair of AIResult and NextActions
     */
    suspend fun processCapture(
        ocrText: String?,
        userMemo: String?,
        emotionLabels: String,
        pastReactions: List<String> = emptyList()
    ): Pair<AIResult, List<NextAction>> {
        val response = if (hasModel) {
            generateReaction(ocrText, userMemo, emotionLabels, pastReactions)
        } else {
            android.util.Log.w("AIService", "No API key, using offline response")
            fallbackResponse(emotionLabels, userMemo)
        }

        val resultId = UUID.randomUUID().toString()
        val aiResult = AIResult(
            id = resultId,
            captureId = UUID.randomUUID().toString(),
            draftSummary = response.draftSummary,
            deepeningQuestion = response.deepeningQuestion,
            axisSelected = null,
            topicTags = response.topicTags.take(5),
            createdAt = Date(),
            updatedAt = Date()
        )

        val nextActions = response.nextActions.take(MAX_NEXT_ACTIONS).mapIndexed { index, dto ->
            NextAction(
                id = UUID.randomUUID().toString(),
                aiResultId = resultId,
                type = dto.type,
                title = dto.title,
                description = dto.description,
                query = dto.query,
                link = dto.link,
                rank = index + 1
            )
        }

        return Pair(aiResult, nextActions)
    }

    private suspend fun generateReaction(
        ocrText: String?,
        userMemo: String?,
        emotionLabels: String,
        pastReactions: List<String>
    ): AIResponse {
        var feedback: String? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val response = try {
                val prompt = AIPrompts.buildReactionPrompt(ocrText, emotionLabels, userMemo, pastReactions, feedback)
                gson.fromJson(AIPrompts.extractJson(callModel(prompt)), AIResponse::class.java)
            } catch (e: Exception) {
                android.util.Log.e("AIService", "Reaction generation failed: ${e.message}")
                return fallbackResponse(emotionLabels, userMemo)
            }
            val violations = AIGuardrails.checkReaction(response.draftSummary, response.deepeningQuestion, ocrText)
            if (violations.isEmpty()) return response
            // Log rule kinds only; the text may contain book wording
            android.util.Log.w("AIService", "Attempt ${attempt + 1} broke guardrails: ${violations.map { it.kind }}")
            feedback = AIGuardrails.feedback(violations)
        }
        return fallbackResponse(emotionLabels, userMemo)
    }

    private fun fallbackResponse(emotionLabels: String, userMemo: String?): AIResponse {
        val (reaction, question) = AIPrompts.fallbackReaction(emotionLabels, userMemo)
        val query = userMemo?.takeIf { it.isNotBlank() }?.take(30)
        return AIResponse(
            draftSummary = reaction,
            deepeningQuestion = question,
            nextActions = listOf(
                NextActionDto("dialog", "AIと対話して深める", "この反応をもう少し言葉にする"),
                NextActionDto("books", "関連書籍を探す", "同じテーマの本を探す", query),
                NextActionDto("videos", "関連動画を探す", "同じテーマの動画を探す", query)
            ),
            topicTags = emptyList()
        )
    }

    /**
     * Chat with AI for deep dive conversation. The AI asks rather than answers.
     *
     * @param context The capture's AI result
     * @param userMessage User's message
     * @return AI response message
     */
    suspend fun chat(context: AIResult, userMessage: String): String {
        if (!hasModel) return offlineChatReply()

        var feedback: String? = null
        repeat(MAX_ATTEMPTS) {
            val reply = try {
                val prompt = AIPrompts.buildChatPrompt(
                    reaction = context.draftSummary,
                    question = context.deepeningQuestion,
                    axisLabel = AIPrompts.axisLabel(context.axisSelected),
                    userMessage = userMessage,
                    feedback = feedback
                )
                callModel(prompt, chat = true).trim()
            } catch (e: Exception) {
                return offlineChatReply()
            }
            val violations = AIGuardrails.checkChat(reply)
            if (violations.isEmpty()) return reply
            feedback = AIGuardrails.feedback(violations)
        }
        return offlineChatReply()
    }

    private fun offlineChatReply(): String =
        "そう考えたのですね。それは、あなたのどんな経験や状況から来ていると感じますか？"

    /**
     * Summarize one book by cutting across the reader's captures.
     * Uses only the reader's own data (stamps, memos, reactions, chosen axes), never book text.
     *
     * @return The formatted summary to store in books.summary
     * @throws IllegalStateException when the AI fails or keeps breaking the rules, so that
     *   a failure message is never saved as the reader's summary
     */
    suspend fun generateBookSummary(
        bookTitle: String,
        contentTypeName: String,
        bookMemo: String?,
        captures: List<BookSummaryCaptureInput>
    ): String {
        require(captures.isNotEmpty()) { "A summary needs at least one capture" }

        if (!hasModel) {
            return AIPrompts.formatBookSummary(AIPrompts.offlineBookSummary(captures))
        }

        var feedback: String? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val prompt = AIPrompts.buildBookSummaryPrompt(bookTitle, contentTypeName, bookMemo, captures, feedback)
            val output = gson.fromJson(AIPrompts.extractJson(callModel(prompt)), BookSummaryOutput::class.java)
            val violations = AIGuardrails.checkBookSummary(output)
            if (violations.isEmpty()) return AIPrompts.formatBookSummary(output)
            android.util.Log.w("AIService", "Summary attempt ${attempt + 1} broke guardrails: ${violations.map { it.kind }}")
            feedback = AIGuardrails.feedback(violations)
        }
        throw IllegalStateException("The summary kept breaking the output rules")
    }
}
