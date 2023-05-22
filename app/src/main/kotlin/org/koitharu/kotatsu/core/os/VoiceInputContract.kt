package org.koitharu.kotatsu.core.os

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.ConfigurationCompat
import java.util.Locale

class VoiceInputContract : ActivityResultContract<String?, String?>() {

	override fun createIntent(context: Context, input: String?): Intent {
		val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
		val locale = ConfigurationCompat.getLocales(context.resources.configuration).get(0) ?: Locale.getDefault()
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, input)
		return intent
	}

	override fun parseResult(resultCode: Int, intent: Intent?): String? {
		return if (resultCode == Activity.RESULT_OK && intent != null) {
			val matches = intent.getStringArrayExtra(RecognizerIntent.EXTRA_RESULTS)
			matches?.firstOrNull()
		} else {
			null
		}
	}
}
