package org.koitharu.kotatsu.core.ui

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import leakcanary.AppWatcher

abstract class BaseService : LifecycleService() {

	override fun attachBaseContext(newBase: Context) {
		super.attachBaseContext(ContextCompat.getContextForLanguage(newBase))
	}

	override fun onDestroy() {
		super.onDestroy()
		AppWatcher.objectWatcher.watch(
			watchedObject = this,
			description = "${javaClass.simpleName} service received Service#onDestroy() callback",
		)
	}
}
