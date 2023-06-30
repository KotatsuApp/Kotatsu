package org.koitharu.kotatsu.core.ui.util

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

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

operator fun ReversibleHandle.plus(other: ReversibleHandle) = ReversibleHandle {
	this.reverse()
	other.reverse()
}
