package org.koitharu.kotatsu.download.domain

import android.os.PowerManager
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class WakeLockNode(
	private val wakeLock: PowerManager.WakeLock,
	private val timeout: Long,
) : AbstractCoroutineContextElement(Key) {

	init {
		wakeLock.setReferenceCounted(true)
	}

	fun acquire() {
		wakeLock.acquire(timeout)
	}

	fun release() {
		wakeLock.release()
	}

	companion object Key : CoroutineContext.Key<WakeLockNode>
}