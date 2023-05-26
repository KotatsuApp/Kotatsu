package org.koitharu.kotatsu.core.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.impl.foreground.SystemForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope

/**
 * Workaround for issue
 * https://issuetracker.google.com/issues/270245927
 * https://issuetracker.google.com/issues/280504155
 */
class WorkServiceStopHelper(
	private val context: Context,
) {

	fun setup() {
		processLifecycleScope.launch(Dispatchers.Default) {
			WorkManager.getInstance(context)
				.getWorkInfosLiveData(WorkQuery.fromStates(WorkInfo.State.RUNNING))
				.asFlow()
				.map { it.isEmpty() }
				.distinctUntilChanged()
				.collectLatest {
					if (it) {
						delay(1_000)
						stopWorkerService()
					}
				}
		}
	}

	@SuppressLint("RestrictedApi")
	private fun stopWorkerService() {
		SystemForegroundService.getInstance()?.stop()
	}
}

