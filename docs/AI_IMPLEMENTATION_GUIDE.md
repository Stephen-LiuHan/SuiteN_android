# AI機能実装ガイド

このドキュメントでは、SuiteNアプリにAI機能を実装するための手順を説明します。

## 概要

現在のアプリはモック実装になっており、実際のAI APIに置き換える必要があります。

### 必要なAI機能

1. **OCRテキスト処理**: スクリーンショットからテキストを抽出し、ユーザーの気づきを整理
2. **チャット機能**: ユーザーとの対話を通じて思考を深掘り

---

## 方法1: OpenAI API（GPT-4）を使用

### 手順

#### 1. APIキーの取得

1. [OpenAI Platform](https://platform.openai.com/) にアクセス
2. アカウントを作成またはログイン
3. API Keys セクションで新しいキーを作成
4. キーを安全な場所に保存

#### 2. 依存関係の追加

`app/build.gradle.kts` に追加:

```kotlin
dependencies {
    // OkHttp for API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Retrofit (optional, for cleaner API calls)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
```

#### 3. AIService の実装

`app/src/main/kotlin/space/suiten/core/service/AIService.kt` を以下のように修正:

```kotlin
package space.suiten.core.service

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AIService(
    private val apiKey: String  // APIキーを渡す
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun processCapture(
        ocrText: String,
        userMemo: String?,
        emotion: String?,
        pastResults: List<String>
    ): AIResponse = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(ocrText, userMemo, emotion, pastResults)

        val requestBody = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                Message("system", SYSTEM_PROMPT),
                Message("user", prompt)
            ),
            temperature = 0.7,
            maxTokens = 1000
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        // Parse response and convert to AIResponse
        // ...
    }

    private fun buildPrompt(
        ocrText: String,
        userMemo: String?,
        emotion: String?,
        pastResults: List<String>
    ): String {
        return """
            以下はユーザーが読書中にキャプチャした内容です。

            【OCRテキスト】
            $ocrText

            【ユーザーのメモ】
            ${userMemo ?: "なし"}

            【感情】
            ${emotion ?: "なし"}

            このキャプチャについて、ユーザーの気づきを整理してください。

            重要な制約:
            - 原文を引用しないでください
            - 「述べている」「書かれている」などの表現は使わないでください
            - 主語は必ずユーザー（「あなた」）にしてください
            - 「感じた」「捉えた」などの暫定的な表現を使ってください

            以下のJSON形式で回答してください:
            {
                "draft_summary": "ユーザーの気づきの要約（250文字以内）",
                "deepening_question": "深掘りのための質問（60文字以内）",
                "next_actions": [
                    {"type": "dialog", "title": "タイトル", "description": "説明", "query": null},
                    {"type": "books", "title": "タイトル", "description": "説明", "query": "検索クエリ"},
                    {"type": "videos", "title": "タイトル", "description": "説明", "query": "検索クエリ"}
                ],
                "topic_tags": ["タグ1", "タグ2"]
            }
        """.trimIndent()
    }

    companion object {
        private const val SYSTEM_PROMPT = """
            あなたは読書支援AIです。ユーザーの読書体験を整理し、思考を深めるお手伝いをします。

            重要なルール:
            1. 本の原文を引用・再現してはいけません
            2. 「書かれている」「述べている」などの表現は禁止です
            3. 主語は必ずユーザー（「あなた」）にしてください
            4. 暫定的な表現（「感じた」「捉えた」「思われた」）を使ってください
            5. ユーザーの反応や気づきを整理することに集中してください
        """
    }
}

// Data classes for API
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("max_tokens") val maxTokens: Int
)

data class Message(
    val role: String,
    val content: String
)
```

#### 4. APIキーの安全な管理

**local.properties** に追加（.gitignore に含まれていることを確認）:
```properties
OPENAI_API_KEY=sk-your-api-key-here
```

**build.gradle.kts** で読み込み:
```kotlin
android {
    defaultConfig {
        val localProperties = project.rootProject.file("local.properties")
        val properties = java.util.Properties()
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }
        buildConfigField("String", "OPENAI_API_KEY", "\"${properties.getProperty("OPENAI_API_KEY", "")}\"")
    }
}
```

---

## 方法2: Claude API（Anthropic）を使用

### 手順

#### 1. APIキーの取得

1. [Anthropic Console](https://console.anthropic.com/) にアクセス
2. アカウントを作成
3. API Keys で新しいキーを作成

#### 2. 実装

OpenAI と同様の構造で、エンドポイントとリクエスト形式を変更:

```kotlin
class ClaudeAIService(private val apiKey: String) {

    suspend fun processCapture(...): AIResponse = withContext(Dispatchers.IO) {
        val requestBody = ClaudeRequest(
            model = "claude-3-sonnet-20240229",
            maxTokens = 1000,
            messages = listOf(
                ClaudeMessage("user", buildPrompt(...))
            ),
            system = SYSTEM_PROMPT
        )

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        // ...
    }
}
```

---

## 方法3: Google Gemini API を使用（推奨）

Googleが提供するGemini APIは、無料枠が充実しており、Androidとの親和性が高いためおすすめです。

### 手順

#### 1. APIキーの取得

1. [Google AI Studio](https://aistudio.google.com/) にアクセス
2. Googleアカウントでログイン
3. 「Get API key」をクリック
4. 「Create API key in new project」または既存プロジェクトを選択
5. 生成されたAPIキーをコピーして安全な場所に保存

#### 2. 依存関係の追加

`app/build.gradle.kts` に追加:

```kotlin
dependencies {
    // Google AI SDK for Android
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
```

#### 3. GeminiAIService の実装

`app/src/main/kotlin/space/suiten/core/service/GeminiAIService.kt` を作成:

```kotlin
package space.suiten.core.service

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import space.suiten.domain.model.AIResult
import space.suiten.domain.model.NextAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class GeminiAIService(
    private val apiKey: String
) {
    private val gson = Gson()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",  // 高速で低コスト
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 1024
        },
        systemInstruction = com.google.ai.client.generativeai.type.content {
            text(SYSTEM_PROMPT)
        }
    )

    /**
     * キャプチャを処理してAI結果を生成
     *
     * CRITICAL: ocrText はこのメソッド内でのみ使用し、保存しないこと
     */
    suspend fun processCapture(
        ocrText: String,
        userMemo: String?,
        emotion: String?,
        pastResults: List<AIResult>
    ): Pair<AIResult, List<NextAction>> = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(ocrText, userMemo, emotion, pastResults)

        val response = generativeModel.generateContent(prompt)
        val responseText = response.text ?: throw IllegalStateException("Empty response from Gemini")

        // JSONをパース
        val jsonResponse = parseJsonResponse(responseText)

        // バリデーション
        validateResponse(jsonResponse)

        // ドメインモデルに変換
        val resultId = UUID.randomUUID().toString()
        val aiResult = AIResult(
            id = resultId,
            captureId = "",  // 呼び出し元で設定
            draftSummary = jsonResponse.draftSummary,
            deepeningQuestion = jsonResponse.deepeningQuestion,
            axisSelected = null,
            topicTags = jsonResponse.topicTags,
            createdAt = Date()
        )

        val nextActions = jsonResponse.nextActions.mapIndexed { index, action ->
            NextAction(
                id = UUID.randomUUID().toString(),
                aiResultId = resultId,
                type = action.type,
                title = action.title,
                description = action.description,
                query = action.query,
                link = null,
                rank = index + 1
            )
        }

        Pair(aiResult, nextActions)
    }

    /**
     * チャットメッセージを処理
     */
    suspend fun chat(
        context: String,
        userMessage: String,
        chatHistory: List<Pair<String, String>>  // (role, message)
    ): String = withContext(Dispatchers.IO) {
        val chat = generativeModel.startChat(
            history = chatHistory.map { (role, message) ->
                com.google.ai.client.generativeai.type.content(role = role) {
                    text(message)
                }
            }
        )

        val contextPrompt = """
            コンテキスト: $context

            ユーザーのメッセージ: $userMessage

            重要: 本の原文を引用せず、ユーザーの思考を深めるサポートをしてください。
        """.trimIndent()

        val response = chat.sendMessage(contextPrompt)
        response.text ?: "応答を生成できませんでした。"
    }

    private fun buildPrompt(
        ocrText: String,
        userMemo: String?,
        emotion: String?,
        pastResults: List<AIResult>
    ): String {
        val pastContext = if (pastResults.isNotEmpty()) {
            pastResults.takeLast(3).joinToString("\n") { result ->
                "- ${result.draftSummary}"
            }
        } else {
            "なし"
        }

        return """
            以下はユーザーが読書中にキャプチャした内容です。

            【OCRテキスト】
            $ocrText

            【ユーザーのメモ】
            ${userMemo ?: "なし"}

            【感情スタンプ】
            ${emotion ?: "なし"}

            【過去の気づき（参考）】
            $pastContext

            このキャプチャについて、ユーザーの気づきを整理してください。

            重要な制約:
            - 原文を引用・再現しないでください
            - 「述べている」「書かれている」「記載されている」などの表現は使わないでください
            - 主語は必ずユーザー（「あなた」）にしてください
            - 「感じた」「捉えた」「思われた」などの暫定的な表現を使ってください
            - ユーザーの反応や気づきを整理することに集中してください

            以下のJSON形式で回答してください（JSONのみ、説明不要）:
            ```json
            {
                "draft_summary": "ユーザーの気づきの要約（250文字以内、主語は「あなた」）",
                "deepening_question": "深掘りのための質問（60文字以内）",
                "next_actions": [
                    {"type": "dialog", "title": "AIと深掘り", "description": "この気づきについてさらに対話する（80文字以内）", "query": null},
                    {"type": "books", "title": "関連書籍を探す", "description": "説明（80文字以内）", "query": "検索クエリ"},
                    {"type": "videos", "title": "関連動画を探す", "description": "説明（80文字以内）", "query": "検索クエリ"}
                ],
                "topic_tags": ["タグ1", "タグ2", "タグ3"]
            }
            ```
        """.trimIndent()
    }

    private fun parseJsonResponse(responseText: String): GeminiResponse {
        // JSONブロックを抽出
        val jsonPattern = Regex("```json\\s*([\\s\\S]*?)\\s*```")
        val match = jsonPattern.find(responseText)
        val jsonString = match?.groupValues?.get(1) ?: responseText

        // snake_case を camelCase にマッピング
        val cleanedJson = jsonString
            .replace("\"draft_summary\"", "\"draftSummary\"")
            .replace("\"deepening_question\"", "\"deepeningQuestion\"")
            .replace("\"next_actions\"", "\"nextActions\"")
            .replace("\"topic_tags\"", "\"topicTags\"")

        return gson.fromJson(cleanedJson, GeminiResponse::class.java)
    }

    private fun validateResponse(response: GeminiResponse) {
        val forbiddenPhrases = listOf(
            "述べている", "書かれている", "記載されている",
            "書いてある", "説明している", "記述されている"
        )

        // 文字数チェック
        require(response.draftSummary.length <= 250) {
            "draft_summary exceeds 250 characters"
        }
        require(response.deepeningQuestion.length <= 60) {
            "deepening_question exceeds 60 characters"
        }

        // 禁止フレーズチェック
        forbiddenPhrases.forEach { phrase ->
            require(!response.draftSummary.contains(phrase)) {
                "draft_summary contains forbidden phrase: $phrase"
            }
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = """
あなたは読書支援AIです。ユーザーの読書体験を整理し、思考を深めるお手伝いをします。

【絶対に守るべきルール】
1. 本の原文を引用・再現してはいけません
2. 以下の表現は禁止です：
   - 「述べている」「書かれている」「記載されている」
   - 「説明している」「記述されている」「書いてある」
3. 主語は必ずユーザー（「あなた」）にしてください
4. 暫定的な表現を使ってください：
   - 「感じた」「捉えた」「思われた」「考えた」
5. ユーザーの反応や気づきを整理することに集中してください

【出力形式】
- 必ず指定されたJSON形式で出力してください
- 文字数制限を必ず守ってください
"""
    }
}

// レスポンス用データクラス
data class GeminiResponse(
    val draftSummary: String,
    val deepeningQuestion: String,
    val nextActions: List<GeminiNextAction>,
    val topicTags: List<String>
)

data class GeminiNextAction(
    val type: String,
    val title: String,
    val description: String,
    val query: String?
)
```

#### 4. AIServiceの置き換え

`SuiteNNavHost.kt` で GeminiAIService を使用:

```kotlin
// Before
val aiService = AIService()

// After
val aiService = GeminiAIService(BuildConfig.GEMINI_API_KEY)
```

#### 5. APIキーの安全な管理

**local.properties** に追加:
```properties
GEMINI_API_KEY=your-api-key-here
```

**app/build.gradle.kts** に追加:
```kotlin
android {
    defaultConfig {
        val localProperties = project.rootProject.file("local.properties")
        val properties = java.util.Properties()
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"${properties.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}
```

#### 6. CaptureViewModel の更新

`CaptureViewModel.kt` の `processWithAI` メソッドを更新:

```kotlin
suspend fun processWithAI(captureId: String): Boolean {
    var ocrText: String? = null

    return try {
        _isLoading.value = true

        val capture = captureRepository.getCaptureById(captureId)
            ?: throw IllegalArgumentException("Capture not found")

        // Update status to running
        val updatedCapture = capture.copy(aiStatus = AIStatus.RUNNING)
        captureRepository.updateCapture(updatedCapture)

        // 1. Extract text from image (IN MEMORY ONLY)
        ocrText = ocrService.extractText(capture.imagePath)

        // 2. Get past results for context
        val pastResults = aiRepository.getRecentResults(3).first()

        // 3. Process with Gemini AI
        val (aiResult, nextActions) = geminiAIService.processCapture(
            ocrText = ocrText,
            userMemo = capture.userMemo,
            emotion = capture.emotion,
            pastResults = pastResults
        )

        // 4. Save AI result (WITHOUT OCR text)
        val finalResult = aiResult.copy(captureId = captureId)
        aiRepository.createAIResult(finalResult, nextActions)

        // 5. CRITICAL: Clear OCR text from memory
        OCRService.clearOCRText(ocrText)
        ocrText = null

        // Update capture status to done
        val doneCapture = capture.copy(aiStatus = AIStatus.DONE)
        captureRepository.updateCapture(doneCapture)

        _isLoading.value = false
        true
    } catch (e: Exception) {
        _error.value = e.message

        // CRITICAL: Clear OCR text even on error
        OCRService.clearOCRText(ocrText)
        ocrText = null

        // Update status to error
        val capture = captureRepository.getCaptureById(captureId)
        if (capture != null) {
            val errorCapture = capture.copy(aiStatus = AIStatus.ERROR)
            captureRepository.updateCapture(errorCapture)
        }

        _isLoading.value = false
        false
    }
}
```

### Gemini API の料金

| モデル | 入力 | 出力 | 無料枠 |
|--------|------|------|--------|
| gemini-1.5-flash | $0.075/1M tokens | $0.30/1M tokens | 15 RPM, 1M tokens/日 |
| gemini-1.5-pro | $1.25/1M tokens | $5.00/1M tokens | 2 RPM, 32K tokens/日 |

**推奨**:
- 開発・テスト: `gemini-1.5-flash`（無料枠で十分）
- 本番: `gemini-1.5-flash`（低コストで高速）

### Gemini のメリット

1. **無料枠が充実**: 1日100万トークンまで無料
2. **高速**: Flash モデルは応答が速い
3. **Android親和性**: Google公式SDKでシームレスに統合
4. **マルチモーダル対応**: 画像を直接送信可能（将来の拡張）

---

## 方法4: サーバー経由でのAPI呼び出し（本番環境推奨）

### メリット

- APIキーをクライアントに含めない（セキュリティ向上）
- レート制限の管理が容易
- コスト管理が容易
- 複数のAIプロバイダーの切り替えが容易

### 構成

```
[Android App] → [Your Backend Server] → [AI API (OpenAI/Claude)]
```

### バックエンドの実装例（Node.js）

```javascript
const express = require('express');
const OpenAI = require('openai');

const app = express();
const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

app.post('/api/process-capture', async (req, res) => {
    const { ocrText, userMemo, emotion } = req.body;

    // Validate request
    // ...

    const completion = await openai.chat.completions.create({
        model: 'gpt-4',
        messages: [
            { role: 'system', content: SYSTEM_PROMPT },
            { role: 'user', content: buildPrompt(ocrText, userMemo, emotion) }
        ]
    });

    res.json(completion.choices[0].message.content);
});

app.listen(3000);
```

### Androidからの呼び出し

```kotlin
class AIService(private val baseUrl: String) {

    suspend fun processCapture(...): AIResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/process-capture")
            .post(/* request body */)
            .build()

        // ...
    }
}
```

---

## プライバシー保護の実装ポイント

### 必須事項

1. **OCRテキストは絶対に保存しない**
   - メモリ内でのみ処理
   - APIに送信後は即座に破棄
   - ログに出力しない

2. **AIの出力を検証**
   - 禁止フレーズのチェック（「述べている」「書かれている」等）
   - 文字数制限の確認
   - 原文再現の検出

```kotlin
fun validateAIResponse(response: AIResponse): Boolean {
    val forbiddenPhrases = listOf(
        "述べている", "書かれている", "記載されている",
        "書いてある", "説明している"
    )

    // Check draft summary
    if (response.draftSummary.length > 250) return false
    if (forbiddenPhrases.any { response.draftSummary.contains(it) }) return false

    // Check question
    if (response.deepeningQuestion.length > 60) return false

    return true
}
```

---

## コスト試算

### Google Gemini（推奨）

| モデル | 入力 | 出力 | 無料枠 |
|--------|------|------|--------|
| gemini-1.5-flash | $0.075/1M tokens | $0.30/1M tokens | 15 RPM, 1M tokens/日 |
| gemini-1.5-pro | $1.25/1M tokens | $5.00/1M tokens | 2 RPM, 32K tokens/日 |

- 1回のキャプチャ処理: **無料**（無料枠内）または約$0.0001〜$0.001

### OpenAI GPT-4

- 入力: $0.03 / 1K tokens
- 出力: $0.06 / 1K tokens
- 1回のキャプチャ処理: 約$0.05〜$0.10

### Claude 3 Sonnet

- 入力: $0.003 / 1K tokens
- 出力: $0.015 / 1K tokens
- 1回のキャプチャ処理: 約$0.01〜$0.02

### 推奨

- **開発/テスト**: Gemini 1.5 Flash（無料枠で十分）
- **本番（低コスト重視）**: Gemini 1.5 Flash
- **本番（品質重視）**: Gemini 1.5 Pro または Claude 3 Sonnet

---

## 実装チェックリスト

- [ ] APIキーの取得
- [ ] 依存関係の追加
- [ ] AIServiceの実装
- [ ] APIキーの安全な管理
- [ ] レスポンス検証の実装
- [ ] エラーハンドリングの実装
- [ ] OCRテキスト破棄の確認
- [ ] ログ出力の確認（OCRテキストが含まれていないこと）
- [ ] テスト（モック → 実API）

---

## トラブルシューティング

### 問題: API呼び出しがタイムアウトする

**解決策**:
- タイムアウト値を増やす（60秒以上推奨）
- ネットワーク接続を確認

### 問題: レスポンスが期待する形式でない

**解決策**:
- プロンプトに明確なJSON形式を指定
- `response_format: { type: "json_object" }` を使用（GPT-4）

### 問題: 禁止フレーズが出力に含まれる

**解決策**:
- システムプロンプトを強化
- 出力後のバリデーションで再試行
