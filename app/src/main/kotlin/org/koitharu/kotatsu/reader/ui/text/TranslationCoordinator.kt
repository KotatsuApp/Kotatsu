package org.koitharu.kotatsu.reader.ui.text

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationCoordinator @Inject constructor(
    private val textRecognitionManager: TextRecognitionManager,
    private val translationManager: TranslationManager,
    private val webBasedTranslator: WebBasedTranslator
) {

    /**
     * Detects text in the image and returns the text elements
     */
    suspend fun detectText(bitmap: Bitmap): List<TextElement> {
        return textRecognitionManager.detectText(bitmap).first()
    }

    /**
     * Translates the text using ML Kit offline translation
     */
    suspend fun translateOffline(text: String, targetLanguage: String): String {
        return translationManager.translate(text, targetLanguage)
    }

    /**
     * Gets the URL for online translation using Google Translate web
     */
    fun getOnlineTranslationUrl(text: String, targetLanguage: String): String {
        return webBasedTranslator.createTranslateUrl(text, targetLanguage)
    }

    /**
     * Checks if a language model is available for offline translation
     */
    suspend fun isLanguageAvailable(languageCode: String): Boolean {
        return translationManager.isLanguageAvailable(languageCode)
    }

    /**
     * Downloads a language model for offline translation
     */
    suspend fun downloadLanguageModel(languageCode: String) {
        translationManager.downloadLanguageModel(languageCode).first()
    }
}
