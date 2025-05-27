package org.koitharu.kotatsu.reader.ui.text

import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationManager @Inject constructor() {

    private val translators = mutableMapOf<String, Translator>()

    suspend fun translate(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val translator = getTranslator("ja", targetLanguage)
        translator.translate(text).await()
    }

    suspend fun isLanguageAvailable(languageCode: String): Boolean {
        return TranslateLanguage.getAllLanguages().contains(languageCode)
    }

    suspend fun downloadLanguageModel(languageCode: String) = callbackFlow {
        val translator = getTranslator("ja", languageCode)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                trySend(true)
                close()
            }
            .addOnFailureListener { error ->
                close(error)
            }
        awaitClose()
    }

    private fun getTranslator(sourceLanguage: String, targetLanguage: String): Translator {
        val key = "$sourceLanguage-$targetLanguage"
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = withContext(Dispatchers.IO) {
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result) { error ->
                    error.printStackTrace()
                }
            }
            addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
            continuation.invokeOnCancellation {
                if (isComplete) return@invokeOnCancellation
                cancel()
            }
        }
    }
}
