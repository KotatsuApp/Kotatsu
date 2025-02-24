package org.koitharu.kotatsu.core.ui

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService

abstract class BaseService : LifecycleService() {

	override fun attachBaseContext(newBase: Context) {
		super.attachBaseContext(ContextCompat.getContextForLanguage(newBase))
	}
}
