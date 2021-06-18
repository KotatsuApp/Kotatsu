package org.koitharu.kotatsu.base.ui.widgets.search.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent

object SearchUtils {

	const val SPEECH_REQUEST_CODE = 300

	@JvmStatic
	fun setVoiceSearch(activity: Activity, text: String) {
		val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(
			RecognizerIntent.EXTRA_LANGUAGE_MODEL,
			RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
		)
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, text)
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

		activity.startActivityForResult(
			intent,
			SPEECH_REQUEST_CODE
		)
	}

	@JvmStatic
	fun isVoiceSearchAvailable(context: Context): Boolean {
		val pm = context.packageManager
		val activities =
			pm.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
		return activities.size != 0
	}

}