package org.koitharu.kotatsu.core.util

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class IdlingDetector(
	private val timeoutMs: Long,
	private val callback: Callback,
) : DefaultLifecycleObserver {

	private val handler = Handler(Looper.getMainLooper())
	private val idleRunnable = Runnable {
		callback.onIdle()
	}

	fun bindToLifecycle(owner: LifecycleOwner) {
		owner.lifecycle.addObserver(this)
	}

	fun onUserInteraction() {
		handler.removeCallbacks(idleRunnable)
		handler.postDelayed(idleRunnable, timeoutMs)
	}

	override fun onDestroy(owner: LifecycleOwner) {
		super.onDestroy(owner)
		owner.lifecycle.removeObserver(this)
		handler.removeCallbacks(idleRunnable)
	}

	fun interface Callback {

		fun onIdle()
	}
}
