package org.koitharu.kotatsu.base.ui

import android.app.Service
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

abstract class CoroutineIntentService : BaseService() {

	private val mutex = Mutex()
	protected open val dispatcher: CoroutineDispatcher = Dispatchers.Default

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		launchCoroutine(intent, startId)
		return Service.START_REDELIVER_INTENT
	}

	private fun launchCoroutine(intent: Intent?, startId: Int) = lifecycleScope.launch {
		mutex.withLock {
			try {
				withContext(dispatcher) {
					processIntent(intent)
				}
			} finally {
				stopSelf(startId)
			}
		}
	}

	protected abstract suspend fun processIntent(intent: Intent?)
}