package org.koitharu.kotatsu.ui.common

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.SparseArray
import androidx.annotation.CallSuper
import androidx.core.util.set
import kotlinx.coroutines.*

abstract class BaseJobService : JobService() {

	private val jobServiceScope = object : CoroutineScope {
		override val coroutineContext = Dispatchers.Main + SupervisorJob()
	}
	private val jobs = SparseArray<Job>(2)

	@CallSuper
	override fun onStartJob(params: JobParameters): Boolean {
		jobs[params.jobId] = jobServiceScope.launch {
			val isSuccess = try {
				doWork(params)
				true
			} catch (_: Throwable) {
				false
			}
			jobFinished(params, !isSuccess)
		}
		return true
	}

	@CallSuper
	override fun onStopJob(params: JobParameters): Boolean {
		val job = jobs[params.jobId] ?: return false
		return !job.isCompleted
	}

	@Throws(Throwable::class)
	protected abstract suspend fun doWork(params: JobParameters)
}