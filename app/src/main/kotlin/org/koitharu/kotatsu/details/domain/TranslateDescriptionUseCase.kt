package org.koitharu.kotatsu.details.domain

import android.text.SpannableStringBuilder
import androidx.core.os.LocaleListCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.prefs.AppSettings
import javax.inject.Inject

class TranslateDescriptionUseCase @Inject constructor(
	private val settings: AppSettings,
) {

	suspend operator fun invoke(text: CharSequence, allowMetered: Boolean): CharSequence? {
		if (text.isBlank()) {
			return null
		}

		return withContext(Dispatchers.Default) {
			try {
				val sourceLanguage = identifyLanguage(text.toString()) ?: return@withContext null
				val targetLanguage = getTargetLanguage() ?: return@withContext null

				if (sourceLanguage == targetLanguage) {
					return@withContext null
				}

				translateText(text.toString(), sourceLanguage, targetLanguage, allowMetered)
			} catch (e: Exception) {
				null
			}
		}
	}

	private suspend fun identifyLanguage(text: String): String? {
		val languageIdentifier = LanguageIdentification.getClient()
		return try {
			val languageCode = languageIdentifier.identifyLanguage(text).await()
			if (languageCode != "und") {
				languageCode
			} else {
				null
			}
		} catch (e: Exception) {
			null
		} finally {
			languageIdentifier.close()
		}
	}

	private suspend fun translateText(
		text: String,
		sourceLanguage: String,
		targetLanguage: String,
		allowMetered: Boolean,
	): CharSequence? {
		val options = TranslatorOptions.Builder()
			.setSourceLanguage(sourceLanguage)
			.setTargetLanguage(targetLanguage)
			.build()

		val translator = Translation.getClient(options)

		return try {
			val conditions = DownloadConditions.Builder()
				.apply {
					if (!allowMetered) {
						requireWifi()
					}
				}
				.build()
			translator.downloadModelIfNeeded(conditions).await()
			val translatedText = translator.translate(text).await()
			SpannableStringBuilder(translatedText)
		} catch (e: Exception) {
			null
		} finally {
			translator.close()
		}
	}

	private fun getTargetLanguage(): String? {
		val locales = settings.appLocales
		val locale = if (locales.isEmpty) {
			LocaleListCompat.getDefault()[0]
		} else {
			locales[0]
		}

		val languageCode = locale?.language ?: return null

		return try {
			TranslateLanguage.fromLanguageTag(languageCode)
		} catch (e: IllegalArgumentException) {
			null
		}
	}
}
