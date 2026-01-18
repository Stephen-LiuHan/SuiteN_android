package space.suiten.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import space.suiten.core.service.AIService
import space.suiten.core.service.ImageService
import space.suiten.core.service.OCRService
import space.suiten.domain.model.AIStatus
import space.suiten.domain.model.Capture
import space.suiten.domain.model.ReadingSession
import space.suiten.domain.repository.AIRepository
import space.suiten.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class CaptureViewModel(
    private val captureRepository: CaptureRepository,
    private val aiRepository: AIRepository,
    private val imageService: ImageService,
    private val ocrService: OCRService,
    private val aiService: AIService
) : ViewModel() {

    private val _captures = MutableStateFlow<List<Capture>>(emptyList())
    val captures: StateFlow<List<Capture>> = _captures.asStateFlow()

    private val _pendingCaptures = MutableStateFlow<List<Capture>>(emptyList())
    val pendingCaptures: StateFlow<List<Capture>> = _pendingCaptures.asStateFlow()

    private val _sessions = MutableStateFlow<List<ReadingSession>>(emptyList())
    val sessions: StateFlow<List<ReadingSession>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // OCR text cache - stores extracted text temporarily for AI processing
    // Key: captureId, Value: extracted OCR text
    private val ocrTextCache = mutableMapOf<String, String>()

    fun loadCapturesBySession(sessionId: String) {
        viewModelScope.launch {
            try {
                captureRepository.getCapturesBySessionId(sessionId).collect { captureList ->
                    _captures.value = captureList
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadCapturesByBook(bookId: String) {
        viewModelScope.launch {
            try {
                captureRepository.getCapturesByBookId(bookId).collect { captureList ->
                    _captures.value = captureList
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadPendingCaptures() {
        viewModelScope.launch {
            try {
                captureRepository.getPendingCaptures().collect { captureList ->
                    _pendingCaptures.value = captureList
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    suspend fun createCapture(
        sessionId: String,
        imageUri: Uri,
        emotion: String?,
        userMemo: String?
    ): Capture? {
        return try {
            _isLoading.value = true

            // Save and compress image
            val imagePath = imageService.saveImage(imageUri)

            val capture = Capture(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                imagePath = imagePath,
                emotion = emotion,
                userMemo = userMemo,
                aiStatus = AIStatus.PENDING,
                createdAt = Date(),
                updatedAt = Date()
            )

            captureRepository.createCapture(capture)
            _isLoading.value = false
            capture
        } catch (e: Exception) {
            _error.value = e.message
            _isLoading.value = false
            null
        }
    }

    /**
     * Create a capture for a book
     * Automatically creates a session if none exists
     */
    suspend fun createCaptureForBook(
        bookId: String,
        imageUri: Uri?,
        emotions: List<String>,
        userMemo: String?
    ): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _isLoading.value = true
                android.util.Log.d("CaptureViewModel", "createCaptureForBook: bookId=$bookId, imageUri=$imageUri")

                // Get or create session for this book
                val sessions = captureRepository.getSessionsByBookId(bookId).first()

                val sessionId = if (sessions.isEmpty()) {
                    // Create a new session
                    val newSession = ReadingSession(
                        id = UUID.randomUUID().toString(),
                        bookId = bookId,
                        startedAt = Date(),
                        endedAt = null,
                        note = null,
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    captureRepository.createSession(newSession)
                    newSession.id
                } else {
                    // Use the most recent session
                    sessions.first().id
                }

                // Save image if provided
                val imagePath = if (imageUri != null) {
                    try {
                        val path = imageService.saveImage(imageUri)
                        android.util.Log.d("CaptureViewModel", "Image saved to: $path")
                        path
                    } catch (e: Exception) {
                        android.util.Log.e("CaptureViewModel", "Failed to save image: ${e.message}", e)
                        ""
                    }
                } else {
                    android.util.Log.w("CaptureViewModel", "No image URI provided")
                    ""
                }

                // Create capture
                val capture = Capture(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    imagePath = imagePath,
                    emotion = emotions.firstOrNull(),
                    userMemo = userMemo,
                    aiStatus = AIStatus.PENDING,
                    createdAt = Date(),
                    updatedAt = Date()
                )

                android.util.Log.d("CaptureViewModel", "Creating capture with imagePath: $imagePath")
                captureRepository.createCapture(capture)
                _isLoading.value = false

                // Start AI processing in background
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    android.util.Log.d("CaptureViewModel", "Starting background AI processing for capture: ${capture.id}")
                    processWithAI(capture.id)
                }

                capture.id // Return capture ID on success
            } catch (e: Exception) {
                android.util.Log.e("CaptureViewModel", "createCaptureForBook failed: ${e.message}", e)
                _error.value = e.message
                _isLoading.value = false
                null // Return null on failure
            }
        }
    }

    /**
     * CRITICAL PRIVACY METHOD
     *
     * Process capture with AI and OCR
     *
     * IMPORTANT:
     * - OCR text exists ONLY in memory during this method execution
     * - OCR text is NEVER stored in database
     * - OCR text is immediately cleared after AI processing
     * - Only AI's abstracted analysis is saved
     */
    suspend fun processWithAI(captureId: String): Boolean {
        var ocrText: String? = null // Scoped ONLY to this method

        return try {
            _isLoading.value = true
            android.util.Log.d("CaptureViewModel", "processWithAI started for captureId: $captureId")

            val capture = captureRepository.getCaptureById(captureId)
            android.util.Log.d("CaptureViewModel", "Capture found: ${capture != null}, imagePath: ${capture?.imagePath}")

            if (capture == null) {
                throw IllegalArgumentException("Capture not found")
            }

            // Update status to running
            val updatedCapture = capture.copy(aiStatus = AIStatus.RUNNING)
            captureRepository.updateCapture(updatedCapture)
            android.util.Log.d("CaptureViewModel", "Status updated to RUNNING")

            // 1. Get OCR text - first check cache, then extract if needed
            val cachedText = ocrTextCache[captureId]
            if (cachedText != null) {
                android.util.Log.d("CaptureViewModel", "Using cached OCR text (${cachedText.length} chars)")
                ocrText = cachedText
            } else if (capture.imagePath.isNotBlank()) {
                android.util.Log.d("CaptureViewModel", "No cached text, starting OCR extraction for image: ${capture.imagePath}")
                try {
                    val file = java.io.File(capture.imagePath)
                    if (file.exists()) {
                        ocrText = ocrService.extractText(capture.imagePath)
                        android.util.Log.d("CaptureViewModel", "OCR completed, text length: ${ocrText?.length ?: 0}")
                        if (ocrText.isNullOrBlank()) {
                            android.util.Log.w("CaptureViewModel", "OCR returned empty text")
                        }
                    } else {
                        android.util.Log.w("CaptureViewModel", "Image file not found: ${capture.imagePath}")
                        ocrText = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CaptureViewModel", "OCR extraction failed: ${e.message}", e)
                    // Continue with AI processing even if OCR fails
                    ocrText = null
                }
            } else {
                android.util.Log.d("CaptureViewModel", "No image path, skipping OCR")
                ocrText = null
            }

            // 2. Process with AI (pass OCR text as parameter, do not store)
            android.util.Log.d("CaptureViewModel", "Starting AI processing...")
            val (aiResult, nextActions) = aiService.processCapture(
                ocrText = ocrText,
                userMemo = capture.userMemo,
                emotion = capture.emotion,
                pastResults = emptyList() // TODO: Load past results if needed
            )
            android.util.Log.d("CaptureViewModel", "AI processing completed, result id: ${aiResult.id}")

            // 3. Save AI result (WITHOUT OCR text)
            val finalResult = aiResult.copy(captureId = captureId)
            android.util.Log.d("CaptureViewModel", "Saving AI result with captureId: $captureId")
            aiRepository.createAIResult(finalResult, nextActions)
            android.util.Log.d("CaptureViewModel", "AI result saved")

            // 4. CRITICAL: Immediately clear OCR text from memory and cache
            OCRService.clearOCRText(ocrText)
            ocrText = null
            ocrTextCache.remove(captureId)

            // Update capture status to done
            val doneCapture = capture.copy(aiStatus = AIStatus.DONE)
            captureRepository.updateCapture(doneCapture)

            android.util.Log.d("CaptureViewModel", "processWithAI completed successfully")
            _isLoading.value = false
            true
        } catch (e: Exception) {
            android.util.Log.e("CaptureViewModel", "processWithAI failed: ${e.message}", e)
            _error.value = e.message

            // CRITICAL: Clear OCR text even on error (from memory and cache)
            OCRService.clearOCRText(ocrText)
            ocrText = null
            ocrTextCache.remove(captureId)

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

    fun updateCapture(capture: Capture) {
        viewModelScope.launch {
            try {
                captureRepository.updateCapture(capture)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteCapture(captureId: String) {
        viewModelScope.launch {
            try {
                val capture = captureRepository.getCaptureById(captureId)
                if (capture != null) {
                    imageService.deleteImage(capture.imagePath)
                    captureRepository.deleteCapture(captureId)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Get capture by ID
     */
    suspend fun getCaptureById(captureId: String): Capture? {
        return captureRepository.getCaptureById(captureId)
    }

    /**
     * Extract text from image and cache it for AI processing
     * Returns the extracted text
     */
    suspend fun extractAndCacheOCRText(captureId: String, imagePath: String): String? {
        return try {
            android.util.Log.d("CaptureViewModel", "extractAndCacheOCRText: Starting OCR for capture $captureId")
            val file = java.io.File(imagePath)
            if (!file.exists()) {
                android.util.Log.w("CaptureViewModel", "extractAndCacheOCRText: Image file not found: $imagePath")
                return null
            }
            val text = ocrService.extractText(imagePath)
            if (!text.isNullOrBlank()) {
                ocrTextCache[captureId] = text
                android.util.Log.d("CaptureViewModel", "extractAndCacheOCRText: Cached OCR text (${text.length} chars) for capture $captureId")
            }
            text
        } catch (e: Exception) {
            android.util.Log.e("CaptureViewModel", "extractAndCacheOCRText: OCR failed: ${e.message}")
            null
        }
    }

    /**
     * Get cached OCR text for a capture
     */
    fun getCachedOCRText(captureId: String): String? {
        return ocrTextCache[captureId]
    }

    /**
     * Clear cached OCR text for a capture
     */
    fun clearCachedOCRText(captureId: String) {
        ocrTextCache.remove(captureId)
        android.util.Log.d("CaptureViewModel", "clearCachedOCRText: Cleared cache for capture $captureId")
    }

    /**
     * Process capture with AI if not already processed
     * Called when AIResultScreen is opened to ensure processing starts
     */
    suspend fun processWithAIIfNeeded(captureId: String): Boolean {
        val capture = captureRepository.getCaptureById(captureId) ?: return false

        return when (capture.aiStatus) {
            AIStatus.PENDING, AIStatus.ERROR -> {
                android.util.Log.d("CaptureViewModel", "processWithAIIfNeeded: Starting AI processing for capture ${capture.id} with status ${capture.aiStatus}")
                processWithAI(captureId)
            }
            AIStatus.RUNNING -> {
                android.util.Log.d("CaptureViewModel", "processWithAIIfNeeded: AI already running for capture ${capture.id}")
                false
            }
            AIStatus.DONE -> {
                android.util.Log.d("CaptureViewModel", "processWithAIIfNeeded: AI already done for capture ${capture.id}")
                true
            }
        }
    }

    /**
     * Re-process capture with AI (force re-analysis)
     * Deletes existing AI result and creates a new one
     */
    suspend fun reprocessWithAI(captureId: String): Boolean {
        android.util.Log.d("CaptureViewModel", "reprocessWithAI: Starting re-analysis for capture $captureId")

        val capture = captureRepository.getCaptureById(captureId) ?: return false

        // Delete existing AI result if any
        try {
            aiRepository.deleteResultByCaptureId(captureId)
            android.util.Log.d("CaptureViewModel", "reprocessWithAI: Deleted existing AI result")
        } catch (e: Exception) {
            android.util.Log.w("CaptureViewModel", "reprocessWithAI: No existing AI result to delete")
        }

        // Reset status to PENDING
        val resetCapture = capture.copy(aiStatus = AIStatus.PENDING)
        captureRepository.updateCapture(resetCapture)

        // Process with AI
        return processWithAI(captureId)
    }

    // Reading Session operations
    fun loadSessionsByBook(bookId: String) {
        viewModelScope.launch {
            try {
                captureRepository.getSessionsByBookId(bookId).collect { sessionList ->
                    _sessions.value = sessionList
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createSession(bookId: String, note: String?) {
        viewModelScope.launch {
            try {
                val session = ReadingSession(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    startedAt = Date(),
                    endedAt = null,
                    note = note,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                captureRepository.createSession(session)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun endSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val session = captureRepository.getSessionById(sessionId)
                if (session != null) {
                    val endedSession = session.copy(endedAt = Date())
                    captureRepository.updateSession(endedSession)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
