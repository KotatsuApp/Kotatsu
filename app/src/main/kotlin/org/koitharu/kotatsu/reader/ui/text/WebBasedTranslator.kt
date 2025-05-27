package org.koitharu.kotatsu.reader.ui.text

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebBasedTranslator @Inject constructor() {

    /**
     * Creates a Google Translate URL for the given text and target language
     */
    fun createTranslateUrl(text: String, targetLanguage: String): String {
        val encodedText = Uri.encode(text)
        return "https://translate.google.com/?sl=auto&tl=$targetLanguage&text=$encodedText&op=translate"
    }

    /**
     * Opens the translation in the browser using the Simple Browser component
     */
    fun openInBrowser(text: String, targetLanguage: String): String {
        return createTranslateUrl(text, targetLanguage)
    }
}
