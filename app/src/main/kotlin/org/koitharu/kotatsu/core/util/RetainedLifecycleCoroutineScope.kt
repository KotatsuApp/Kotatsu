package org.koitharu.kotatsu.core.util

import dagger.hilt.android.lifecycle.RetainedLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class RetainedLifecycleCoroutineScope(
	val lifecycle: RetainedLifecycle,
) : CoroutineScope, RetainedLifecycle.OnClearedListener {

	override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate

	init {
		lifecycle.addOnClearedListener(this)
	}

	override fun onCleared() {
		coroutineContext.cancel()
	}
}
