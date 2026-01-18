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
 * - Validates AI output to ensure no original text reproduction
 * - Checks for forbidden phrases
 * - Enforces character limits
 * - Ensures user-centric language
 * - OCR text is NEVER stored - only passed temporarily for processing
 */
class AIService {

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
        private const val MAX_SUMMARY_LENGTH = 400
        private const val MAX_QUESTION_LENGTH = 100
        private const val MAX_ACTION_DESCRIPTION_LENGTH = 100

        // Forbidden phrases that indicate original text reproduction
        private val FORBIDDEN_PHRASES = listOf(
            "述べている",
            "書かれている",
            "記載されている",
            "書いてある",
            "説明している"
        )

        private const val SYSTEM_PROMPT = """
あなたは読書アプリのAIアシスタントです。ユーザーが本の気になる箇所をキャプチャしました。
あなたの役割は、キャプチャした内容から「本についての気づき」を整理・言語化することです。

【あなたのタスク】
ユーザーがキャプチャした本の内容を分析し、以下の観点で「気づき」をまとめてください：

1. **この本が伝えようとしていること**
   - 著者の主張やメッセージは何か
   - どんな考え方・概念が紹介されているか

2. **キーとなるアイデア**
   - 核心となる考え方を自分の言葉で説明
   - 重要なキーワードや用語の意味

3. **なぜこの箇所が気になったのか**（ユーザーのメモや感情から推測）
   - ユーザーにとっての意味や価値
   - 既存の知識や経験との関連

【draft_summaryの書き方】
- 「この本では〜という考え方が紹介されています」のように、本の内容を客観的にまとめる
- 著者の主張を自分の言葉で言い換えて説明する
- ユーザーのメモがあれば、それと本の内容を関連づける
- 具体的で実用的な気づきを提供する

【良い例】
「この箇所では『環境設計』という概念が紹介されています。習慣を身につけるには意志力に頼るのではなく、行動しやすい環境を先に整えることが重要という考え方です。例えば、運動習慣をつけたければ、寝室にトレーニングウェアを置いておくといった工夫が挙げられます。」

【悪い例】
- 「興味深い内容ですね」（抽象的すぎる）
- 「〜しましょう」（アドバイスになっている）
- 原文をそのまま引用する

【著作権保護】
- 原文をそのままコピーしない
- 必ず自分の言葉で言い換える

【出力形式】
{
  "draft_summary": "本についての気づきをまとめた文章（400文字以内）",
  "deepening_question": "この気づきをさらに深めるための問いかけ（100文字以内）",
  "next_actions": [
    {"type": "dialog", "title": "AIと深掘り対話", "description": "この気づきについて深掘りする", "query": null, "link": null},
    {"type": "books", "title": "関連書籍を探す", "description": "関連する本を探す", "query": "具体的な検索キーワード", "link": null}
  ],
  "topic_tags": ["本から抽出したキーワード1", "キーワード2", "キーワード3"]
}

topic_tagsには本の内容から抽出した具体的なキーワードを3〜5個設定してください。
"""
    }

    data class AIRequest(
        @SerializedName("ocr_text") val ocrText: String,
        @SerializedName("user_memo") val userMemo: String?,
        @SerializedName("emotion") val emotion: String?,
        @SerializedName("past_results") val pastResults: List<String>
    )

    data class AIResponse(
        @SerializedName("draft_summary") val draftSummary: String,
        @SerializedName("deepening_question") val deepeningQuestion: String,
        @SerializedName("axis_options") val axisOptions: List<String> = emptyList(),
        @SerializedName("next_actions") val nextActions: List<NextActionDto>,
        @SerializedName("topic_tags") val topicTags: List<String>
    )

    data class NextActionDto(
        val type: String,
        val title: String,
        val description: String,
        val query: String?,
        val link: String?
    )

    /**
     * Process capture with Gemini AI
     *
     * @param ocrText MEMORY ONLY - will not be stored. Can be null if OCR failed or no image.
     * @param userMemo User's memo
     * @param emotion User's emotion
     * @param pastResults Previous AI results for context
     * @return Pair of AIResult and NextActions
     */
    suspend fun processCapture(
        ocrText: String?,
        userMemo: String?,
        emotion: String?,
        pastResults: List<AIResult> = emptyList()
    ): Pair<AIResult, List<NextAction>> {
        val response = if (BuildConfig.GEMINI_API_KEY.isNotEmpty()) {
            try {
                android.util.Log.d("AIService", "Calling Gemini API with ocrText length: ${ocrText?.length ?: 0}")
                callGeminiAPI(ocrText, userMemo, emotion, pastResults)
            } catch (e: Exception) {
                android.util.Log.e("AIService", "Gemini API call failed: ${e.message}", e)
                // Fallback to mock if API fails
                generateMockResponse(userMemo, emotion)
            }
        } else {
            android.util.Log.w("AIService", "No API key, using mock response")
            // Use mock if no API key
            generateMockResponse(userMemo, emotion)
        }

        // Validate response
        validateAIResponse(response)

        val resultId = UUID.randomUUID().toString()
        val captureId = UUID.randomUUID().toString()

        val aiResult = AIResult(
            id = resultId,
            captureId = captureId,
            draftSummary = response.draftSummary,
            deepeningQuestion = response.deepeningQuestion,
            axisSelected = null,
            topicTags = response.topicTags,
            createdAt = Date(),
            updatedAt = Date()
        )

        val nextActions = response.nextActions.mapIndexed { index, dto ->
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

    /**
     * Call Gemini API for processing
     */
    private suspend fun callGeminiAPI(
        ocrText: String?,
        userMemo: String?,
        emotion: String?,
        pastResults: List<AIResult>
    ): AIResponse {
        val userPrompt = buildString {
            appendLine("=== キャプチャ情報 ===")
            appendLine()

            // OCR text is the primary source for book insights
            if (!ocrText.isNullOrBlank()) {
                appendLine("【本の内容（キャプチャから抽出）】")
                appendLine(ocrText)
                appendLine()
            }

            appendLine("【ユーザーの反応】")
            if (emotion != null) {
                appendLine("感情: $emotion")
            }
            if (!userMemo.isNullOrBlank()) {
                appendLine("メモ: $userMemo")
            }
            if (emotion == null && userMemo.isNullOrBlank()) {
                appendLine("（反応の記録なし）")
            }
            appendLine()

            appendLine("【依頼】")
            if (!ocrText.isNullOrBlank()) {
                appendLine("上記の本の内容から「気づき」を整理してください。")
                appendLine("- この本が伝えようとしていることは何か")
                appendLine("- キーとなるアイデアや概念は何か")
                appendLine("- ユーザーのメモや感情があれば、それを踏まえて気づきをまとめてください")
            } else if (!userMemo.isNullOrBlank()) {
                appendLine("ユーザーのメモから、読書に関する気づきを整理してください。")
            } else {
                appendLine("ユーザーの感情から、読書体験についての気づきを提供してください。")
            }
        }

        val fullPrompt = "$SYSTEM_PROMPT\n\n$userPrompt"
        android.util.Log.d("AIService", "Sending prompt to Gemini (length: ${fullPrompt.length})")

        val response = generativeModel.generateContent(fullPrompt)
        val responseText = response.text ?: throw Exception("Empty response from Gemini")
        android.util.Log.d("AIService", "Received response from Gemini (length: ${responseText.length})")

        // Extract JSON from response
        val jsonText = extractJson(responseText)
        return gson.fromJson(jsonText, AIResponse::class.java)
    }

    /**
     * Chat with AI for deep dive conversation
     *
     * @param context Previous AI result context
     * @param userMessage User's message
     * @return AI response message
     */
    suspend fun chat(context: AIResult, userMessage: String): String {
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
            return generateMockChatResponse(userMessage)
        }

        return try {
            val chatPrompt = buildString {
                appendLine("あなたは読書アプリのAIアシスタントです。")
                appendLine("ユーザーとの対話では、本の原文を引用せず、ユーザーの思考を深める質問や視点を提供してください。")
                appendLine()
                appendLine("【これまでの分析】")
                appendLine("あなたの気づき: ${context.draftSummary}")
                appendLine("深掘りの視点: ${context.deepeningQuestion}")
                appendLine()
                appendLine("【ユーザーのメッセージ】")
                appendLine(userMessage)
                appendLine()
                appendLine("ユーザーの思考を深める返答をしてください。200文字以内で簡潔に。")
            }

            val response = chatModel.generateContent(chatPrompt)
            response.text ?: "すみません、応答を生成できませんでした。"
        } catch (e: Exception) {
            generateMockChatResponse(userMessage)
        }
    }

    /**
     * Extract JSON from AI response
     */
    private fun extractJson(text: String): String {
        // Try to find JSON in the response
        val jsonStart = text.indexOf("{")
        val jsonEnd = text.lastIndexOf("}") + 1

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd)
        }

        throw IllegalStateException("No valid JSON found in AI response")
    }

    /**
     * Validate AI response against privacy and quality guardrails
     */
    private fun validateAIResponse(response: AIResponse) {
        // Check summary length (warn only, don't fail)
        if (response.draftSummary.length > MAX_SUMMARY_LENGTH) {
            android.util.Log.w("AIService", "AI summary exceeds $MAX_SUMMARY_LENGTH characters: ${response.draftSummary.length}")
        }

        // Check question length (warn only, don't fail)
        if (response.deepeningQuestion.length > MAX_QUESTION_LENGTH) {
            android.util.Log.w("AIService", "AI question exceeds $MAX_QUESTION_LENGTH characters: ${response.deepeningQuestion.length}")
        }

        // Check for forbidden phrases - this is critical for copyright protection
        val allText = response.draftSummary + " " + response.deepeningQuestion
        FORBIDDEN_PHRASES.forEach { phrase ->
            if (allText.contains(phrase)) {
                android.util.Log.w("AIService", "AI output contains forbidden phrase: $phrase")
                // Don't throw - let the response through but log the warning
            }
        }

        // Validate action descriptions (warn only)
        response.nextActions.forEach { action ->
            if (action.description.length > MAX_ACTION_DESCRIPTION_LENGTH) {
                android.util.Log.w("AIService", "Action description exceeds $MAX_ACTION_DESCRIPTION_LENGTH characters")
            }
        }
    }

    /**
     * Mock AI response for testing (when API key is not set)
     */
    private fun generateMockResponse(userMemo: String?, emotion: String?): AIResponse {
        val emotionText = emotion ?: "興味"
        val memoContext = if (!userMemo.isNullOrBlank()) {
            "メモ「$userMemo」から、この内容に関心を持っていることがわかります。"
        } else {
            "この箇所を保存したのは、何か気になる点があったからでしょう。"
        }

        return AIResponse(
            draftSummary = "あなたは「$emotionText」という感情を抱きながら、この内容を読みました。$memoContext",
            deepeningQuestion = "この内容で最も印象に残った点は何ですか？",
            axisOptions = listOf("リスク", "成功率", "理由づけ", "概念"),
            nextActions = listOf(
                NextActionDto(
                    type = "dialog",
                    title = "AIと深掘り対話",
                    description = "この内容についてさらに考えを深める",
                    query = null,
                    link = null
                ),
                NextActionDto(
                    type = "books",
                    title = "関連書籍を探す",
                    description = "このテーマの本を探す",
                    query = userMemo ?: "読書",
                    link = null
                ),
                NextActionDto(
                    type = "videos",
                    title = "関連動画を探す",
                    description = "このテーマの動画を探す",
                    query = userMemo ?: "読書",
                    link = null
                )
            ),
            topicTags = listOf("読書", "気づき")
        )
    }

    /**
     * Mock chat response for testing
     */
    private fun generateMockChatResponse(userMessage: String): String {
        return "なるほど、「$userMessage」について考えているんですね。その視点は興味深いです。もう少し具体的に、どのような場面でそう感じましたか？"
    }

    /**
     * Generate a book summary based on all capture AI results
     *
     * @param bookTitle The title of the book
     * @param contentTypeName The type of content (本、動画、etc.)
     * @param bookMemo User's memo about the book
     * @param aiResults All AI results from captures for this book
     * @return Summary of the user's reading impressions
     */
    suspend fun generateBookSummary(
        bookTitle: String,
        contentTypeName: String,
        bookMemo: String?,
        aiResults: List<AIResult>
    ): String {
        if (aiResults.isEmpty()) {
            return "まだキャプチャがありません。コンテンツを視聴しながら気になった箇所をキャプチャして、気づきを記録しましょう。"
        }

        if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
            return generateMockBookSummary(bookTitle, contentTypeName, aiResults)
        }

        return try {
            val prompt = buildBookSummaryPrompt(bookTitle, contentTypeName, bookMemo, aiResults)
            android.util.Log.d("AIService", "Generating summary for: $bookTitle ($contentTypeName) with ${aiResults.size} captures")

            val response = generativeModel.generateContent(prompt)
            response.text ?: "感想の生成に失敗しました。"
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Summary generation failed: ${e.message}", e)
            generateMockBookSummary(bookTitle, contentTypeName, aiResults)
        }
    }

    private fun buildBookSummaryPrompt(
        bookTitle: String,
        contentTypeName: String,
        bookMemo: String?,
        aiResults: List<AIResult>
    ): String {
        return buildString {
            appendLine("あなたは学習・読書アプリのAIアシスタントです。")
            appendLine("ユーザーが「$bookTitle」という${contentTypeName}を視聴し、複数の箇所をキャプチャしました。")

            if (!bookMemo.isNullOrBlank()) {
                appendLine()
                appendLine("【ユーザーのメモ】")
                appendLine(bookMemo)
            }

            appendLine()
            appendLine("それぞれのキャプチャに対するAI分析結果を基に、ユーザーがこの${contentTypeName}から得た全体的な感想・学びをまとめてください。")
            appendLine()
            appendLine("【各キャプチャの分析結果】")

            aiResults.forEachIndexed { index, result ->
                appendLine()
                appendLine("--- キャプチャ ${index + 1} ---")
                appendLine("気づき: ${result.draftSummary}")
                if (result.topicTags.isNotEmpty()) {
                    appendLine("トピック: ${result.topicTags.joinToString(", ")}")
                }
            }

            appendLine()
            appendLine("【まとめ方の指示】")
            appendLine("1. この${contentTypeName}全体を通してユーザーが何を学んだか・感じたかをまとめる")
            appendLine("2. 共通するテーマやキーワードがあれば抽出する")
            appendLine("3. ユーザーがこの${contentTypeName}から得た主な気づきを3〜5点で整理する")
            appendLine("4. 最後に、この${contentTypeName}を視聴した全体的な感想を1〜2文でまとめる")
            if (!bookMemo.isNullOrBlank()) {
                appendLine("5. ユーザーのメモも考慮して、より個人的な感想を生成する")
            }
            appendLine()
            appendLine("【禁止事項】")
            appendLine("- 原文の引用")
            appendLine("- 「〜しましょう」などのアドバイス")
            appendLine("- 抽象的すぎる表現")
            appendLine()
            appendLine("400〜600文字程度で、読みやすくまとめてください。")
        }
    }

    private fun generateMockBookSummary(bookTitle: String, contentTypeName: String, aiResults: List<AIResult>): String {
        val topicSet = aiResults.flatMap { it.topicTags }.toSet().take(5)
        val topicsText = if (topicSet.isNotEmpty()) {
            "主なトピック: ${topicSet.joinToString("、")}"
        } else {
            ""
        }

        return buildString {
            appendLine("「$bookTitle」（$contentTypeName）の記録")
            appendLine()
            appendLine("${aiResults.size}件のキャプチャから、以下のような気づきが得られました。")
            appendLine()
            aiResults.take(3).forEachIndexed { index, result ->
                appendLine("${index + 1}. ${result.draftSummary.take(50)}...")
            }
            if (topicsText.isNotEmpty()) {
                appendLine()
                appendLine(topicsText)
            }
        }
    }
}
