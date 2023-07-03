package org.koitharu.kotatsu.core.util

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ContinuationResumeRunnable(
	private val continuation: Continuation<Unit>,
) : Runnable {

	override fun run() {
		continuation.resume(Unit)
	}
}
