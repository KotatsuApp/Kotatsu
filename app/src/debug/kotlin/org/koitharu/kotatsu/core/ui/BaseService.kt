package org.koitharu.kotatsu.core.ui

import androidx.lifecycle.LifecycleService
import leakcanary.AppWatcher

abstract class BaseService : LifecycleService() {

	override fun onDestroy() {
		super.onDestroy()
		AppWatcher.objectWatcher.watch(
			watchedObject = this,
			description = "${javaClass.simpleName} service received Service#onDestroy() callback",
		)
	}
}
