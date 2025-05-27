package org.koitharu.kotatsu.reader.ui.text

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val translationCoordinator: TranslationCoordinator
) : ViewModel() {

    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState

    private val _detectedTexts = MutableStateFlow<List<TextElement>>(emptyList())
    val detectedTexts: StateFlow<List<TextElement>> = _detectedTexts

    private val _currentTranslation = MutableStateFlow<String?>(null)
    val currentTranslation: StateFlow<String?> = _currentTranslation

    fun detectText(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _translationState.value = TranslationState.Loading
                val texts = translationCoordinator.detectText(bitmap)
                _detectedTexts.value = texts
                _translationState.value = TranslationState.TextDetected
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun translateOffline(text: String, targetLanguage: String) {
        viewModelScope.launch {
            try {
                _translationState.value = TranslationState.Loading
                val translation = translationCoordinator.translateOffline(text, targetLanguage)
                _currentTranslation.value = translation
                _translationState.value = TranslationState.Translated
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getOnlineTranslationUrl(text: String, targetLanguage: String): String {
        return translationCoordinator.getOnlineTranslationUrl(text, targetLanguage)
    }

    fun downloadLanguageModel(languageCode: String) {
        viewModelScope.launch {
            try {
                _translationState.value = TranslationState.Loading
                translationCoordinator.downloadLanguageModel(languageCode)
                _translationState.value = TranslationState.ModelDownloaded
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class TranslationState {
    data object Idle : TranslationState()
    data object Loading : TranslationState()
    data object TextDetected : TranslationState()
    data object Translated : TranslationState()
    data object ModelDownloaded : TranslationState()
    data class Error(val message: String) : TranslationState()
}
