package org.koitharu.kotatsu.core.ui

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

abstract class CoroutineIntentService : BaseService() {

	private val mutex = Mutex()
	protected open val dispatcher: CoroutineDispatcher = Dispatchers.Default

	final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		val job = launchCoroutine(intent, startId)
		val receiver = CancelReceiver(job)
		ContextCompat.registerReceiver(
			this,
			receiver,
			createIntentFilter(this, startId),
			ContextCompat.RECEIVER_NOT_EXPORTED,
		)
		job.invokeOnCompletion { unregisterReceiver(receiver) }
		return START_REDELIVER_INTENT
	}

	private fun launchCoroutine(intent: Intent?, startId: Int) = lifecycleScope.launch(errorHandler(startId)) {
		mutex.withLock {
			try {
				if (intent != null) {
					withContext(dispatcher) {
						processIntent(startId, intent)
					}
				}
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				onError(startId, e)
			} finally {
				stopSelf(startId)
			}
		}
	}

	@WorkerThread
	protected abstract suspend fun processIntent(startId: Int, intent: Intent)

	@AnyThread
	protected abstract fun onError(startId: Int, error: Throwable)

	protected fun getCancelIntent(startId: Int) = PendingIntentCompat.getBroadcast(
		this,
		0,
		createCancelIntent(this, startId),
		PendingIntent.FLAG_UPDATE_CURRENT,
		false,
	)

	private fun errorHandler(startId: Int) = CoroutineExceptionHandler { _, throwable ->
		throwable.printStackTraceDebug()
		onError(startId, throwable)
	}

	private class CancelReceiver(
		private val job: Job
	) : BroadcastReceiver() {

		override fun onReceive(context: Context?, intent: Intent?) {
			job.cancel()
		}
	}

	private companion object {

		private const val SCHEME = "startid"
		private const val ACTION_SUFFIX_CANCEL = ".ACTION_CANCEL"

		fun createIntentFilter(service: CoroutineIntentService, startId: Int): IntentFilter {
			val intentFilter = IntentFilter(cancelAction(service))
			intentFilter.addDataScheme(SCHEME)
			intentFilter.addDataPath(startId.toString(), PatternMatcher.PATTERN_LITERAL)
			return intentFilter
		}

		fun createCancelIntent(service: CoroutineIntentService, startId: Int): Intent {
			return Intent(cancelAction(service))
				.setData("$SCHEME://$startId".toUri())
		}

		private fun cancelAction(service: CoroutineIntentService) = service.javaClass.name + ACTION_SUFFIX_CANCEL
	}
}
