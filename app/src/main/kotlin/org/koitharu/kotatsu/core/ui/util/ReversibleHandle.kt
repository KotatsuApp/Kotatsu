package org.koitharu.kotatsu.core.ui.util

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable

fun interface ReversibleHandle {

	suspend fun reverse()
}

fun ReversibleHandle.reverseAsync() = processLifecycleScope.launch(Dispatchers.Default, CoroutineStart.ATOMIC) {
	runCatchingCancellable {
		withContext(NonCancellable) {
			reverse()
		}
	}.onFailure {
		it.printStackTraceDebug()
	}
}
