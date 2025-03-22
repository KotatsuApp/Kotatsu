package org.koitharu.kotatsu.core.util

import dagger.hilt.android.lifecycle.RetainedLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class RetainedLifecycleCoroutineScope(
	val lifecycle: RetainedLifecycle,
) : CoroutineScope, RetainedLifecycle.OnClearedListener {

	override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate

	init {
		launch(Dispatchers.Main.immediate) {
			lifecycle.addOnClearedListener(this@RetainedLifecycleCoroutineScope)
		}
	}

	override fun onCleared() {
		coroutineContext.cancel()
	}
}
