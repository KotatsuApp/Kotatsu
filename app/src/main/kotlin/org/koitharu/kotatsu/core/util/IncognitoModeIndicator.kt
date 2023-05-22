package org.koitharu.kotatsu.core.util

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.DefaultActivityLifecycleCallbacks
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncognitoModeIndicator @Inject constructor(
	private val settings: AppSettings,
) : DefaultActivityLifecycleCallbacks {

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		if (activity !is AppCompatActivity) {
			return
		}
		settings.observeAsFlow(
			key = AppSettings.KEY_INCOGNITO_MODE,
			valueProducer = { isIncognitoModeEnabled },
		).flowOn(Dispatchers.IO)
			.flowWithLifecycle(activity.lifecycle)
			.onEach { updateStatusBar(activity, it) }
			.launchIn(activity.lifecycleScope)
	}

	private fun updateStatusBar(activity: AppCompatActivity, isIncognitoModeEnabled: Boolean) {
		activity.window.statusBarColor = if (isIncognitoModeEnabled) {
			ContextCompat.getColor(activity, R.color.status_bar_incognito)
		} else {
			activity.getThemeColor(android.R.attr.statusBarColor)
		}
	}
}
