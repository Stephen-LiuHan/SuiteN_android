package space.suiten.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import space.suiten.core.service.AIService
import space.suiten.domain.model.AIResult
import space.suiten.domain.model.NextAction
import space.suiten.domain.model.UserProfile
import space.suiten.domain.repository.AIRepository
import space.suiten.presentation.screens.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class AIViewModel(
    private val aiRepository: AIRepository,
    private val aiService: AIService
) : ViewModel() {

    private val _currentAIResult = MutableStateFlow<AIResult?>(null)
    val currentAIResult: StateFlow<AIResult?> = _currentAIResult.asStateFlow()

    private val _nextActions = MutableStateFlow<List<NextAction>>(emptyList())
    val nextActions: StateFlow<List<NextAction>> = _nextActions.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isPersonalizationEnabled = MutableStateFlow(true)
    val isPersonalizationEnabled: StateFlow<Boolean> = _isPersonalizationEnabled.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadAIResultByCaptureId(captureId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = aiRepository.getResultByCaptureId(captureId)
                _currentAIResult.value = result

                if (result != null) {
                    loadNextActions(result.id)
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadAIResultById(resultId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = aiRepository.getResultById(resultId)
                _currentAIResult.value = result

                if (result != null) {
                    loadNextActions(result.id)
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun loadNextActions(resultId: String) {
        viewModelScope.launch {
            try {
                aiRepository.getActionsByResultId(resultId).collect { actions ->
                    _nextActions.value = actions
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                aiRepository.getUserProfile().collect { profile ->
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun selectAxis(axis: String) {
        viewModelScope.launch {
            try {
                val result = _currentAIResult.value ?: return@launch
                val updatedResult = result.copy(axisSelected = axis)
                aiRepository.updateAIResult(updatedResult)
                _currentAIResult.value = updatedResult

                // Update user profile preferences
                updateAxisPreference(axis)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private suspend fun updateAxisPreference(axis: String) {
        try {
            val currentProfile = _userProfile.value ?: UserProfile(
                id = "me",
                axisPreferences = emptyMap(),
                actionPreferences = emptyMap(),
                updatedAt = Date()
            )

            val updatedAxisPrefs = currentProfile.axisPreferences.toMutableMap()
            updatedAxisPrefs[axis] = (updatedAxisPrefs[axis] ?: 0) + 1

            val updatedProfile = currentProfile.copy(
                axisPreferences = updatedAxisPrefs,
                updatedAt = Date()
            )
            aiRepository.updateUserProfile(updatedProfile)
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    fun recordActionClick(actionType: String) {
        viewModelScope.launch {
            try {
                val currentProfile = _userProfile.value ?: UserProfile(
                    id = "me",
                    axisPreferences = emptyMap(),
                    actionPreferences = emptyMap(),
                    updatedAt = Date()
                )

                val updatedActionPrefs = currentProfile.actionPreferences.toMutableMap()
                updatedActionPrefs[actionType] = (updatedActionPrefs[actionType] ?: 0) + 1

                val updatedProfile = currentProfile.copy(
                    actionPreferences = updatedActionPrefs,
                    updatedAt = Date()
                )
                aiRepository.updateUserProfile(updatedProfile)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun togglePersonalization(enabled: Boolean) {
        _isPersonalizationEnabled.value = enabled
    }

    // Chat methods
    fun initializeChat(resultId: String) {
        _chatMessages.value = emptyList()
        loadAIResultById(resultId)

        // Add initial AI message based on context
        val result = _currentAIResult.value
        if (result != null) {
            val initialMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "「${result.draftSummary}」についてさらに深掘りしましょう。何について詳しく考えたいですか？",
                isUser = false
            )
            _chatMessages.value = listOf(initialMessage)
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = message,
                isUser = true
            )
            _chatMessages.value = _chatMessages.value + userMessage

            _isChatLoading.value = true

            try {
                // Use Gemini AI for response
                val context = _currentAIResult.value
                val aiResponse = if (context != null) {
                    aiService.chat(context, message)
                } else {
                    "申し訳ありません。コンテキストが見つかりませんでした。"
                }

                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = aiResponse,
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + aiMessage
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "エラーが発生しました: ${e.message}",
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + errorMessage
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun sendQuickAction(prompt: String) {
        sendMessage(prompt)
        recordActionClick("dialog")
    }

    fun saveFeedback(captureId: String, helpfulness: Int) {
        viewModelScope.launch {
            try {
                aiRepository.saveFeedback(captureId, helpfulness)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Generate a summary of the book based on all capture AI results
     */
    suspend fun generateBookSummary(
        bookTitle: String,
        contentTypeName: String,
        bookMemo: String?,
        captureIds: List<String>
    ): String {
        return try {
            // Collect AI results for all captures
            val aiResults = captureIds.mapNotNull { captureId ->
                aiRepository.getResultByCaptureId(captureId)
            }

            aiService.generateBookSummary(bookTitle, contentTypeName, bookMemo, aiResults)
        } catch (e: Exception) {
            android.util.Log.e("AIViewModel", "Failed to generate summary: ${e.message}")
            "感想の生成に失敗しました: ${e.message}"
        }
    }
}
