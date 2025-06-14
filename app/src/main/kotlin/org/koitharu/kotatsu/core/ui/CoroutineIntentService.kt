package org.koitharu.kotatsu.core.ui

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

abstract class CoroutineIntentService : BaseService() {

	private val mutex = Mutex()

	final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		launchCoroutine(intent, startId)
		return START_REDELIVER_INTENT
	}

	private fun launchCoroutine(intent: Intent?, startId: Int) = lifecycleScope.launch {
		val intentJobContext = IntentJobContextImpl(startId, this)
		mutex.withLock {
			try {
				if (intent != null) {
					withContext(Dispatchers.Default) {
						intentJobContext.processIntent(intent)
					}
				}
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				intentJobContext.onError(e)
			} finally {
				intentJobContext.stop()
			}
		}
	}

	@WorkerThread
	protected abstract suspend fun IntentJobContext.processIntent(intent: Intent)

	@AnyThread
	protected abstract fun IntentJobContext.onError(error: Throwable)

	interface IntentJobContext : CoroutineScope {

		val startId: Int

		fun getCancelIntent(): PendingIntent?

		fun setForeground(id: Int, notification: Notification, serviceType: Int)
	}

	protected inner class IntentJobContextImpl(
		override val startId: Int,
		private val scope: CoroutineScope,
	) : IntentJobContext, CoroutineScope by scope {

		private var cancelReceiver: CancelReceiver? = null
		private var isStopped = false
		private var isForeground = false

		override fun getCancelIntent(): PendingIntent? {
			ensureHasCancelReceiver()
			return PendingIntentCompat.getBroadcast(
				applicationContext,
				0,
				createCancelIntent(this@CoroutineIntentService, startId),
				PendingIntent.FLAG_UPDATE_CURRENT,
				false,
			)
		}

		override fun setForeground(id: Int, notification: Notification, serviceType: Int) {
			ServiceCompat.startForeground(this@CoroutineIntentService, id, notification, serviceType)
			isForeground = true
		}

		fun stop() {
			synchronized(this) {
				cancelReceiver?.let {
					try {
						unregisterReceiver(it)
					} catch (e: IllegalArgumentException) {
						e.printStackTraceDebug()
					}
				}
				isStopped = true
			}
			if (isForeground) {
				ServiceCompat.stopForeground(this@CoroutineIntentService, ServiceCompat.STOP_FOREGROUND_REMOVE)
			}
			stopSelf(startId)
		}

		private fun ensureHasCancelReceiver() {
			if (cancelReceiver == null && !isStopped) {
				synchronized(this) {
					if (cancelReceiver == null && !isStopped) {
						val job = coroutineContext[Job] ?: return
						CancelReceiver(job).let { receiver ->
							ContextCompat.registerReceiver(
								applicationContext,
								receiver,
								createIntentFilter(this@CoroutineIntentService, startId),
								ContextCompat.RECEIVER_NOT_EXPORTED,
							)
							cancelReceiver = receiver
						}
					}
				}
			}
		}
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
