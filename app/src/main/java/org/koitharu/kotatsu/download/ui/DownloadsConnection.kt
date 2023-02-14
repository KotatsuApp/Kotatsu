package org.koitharu.kotatsu.download.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.utils.asFlowLiveData
import org.koitharu.kotatsu.utils.progress.PausingProgressJob

class DownloadsConnection(
	private val context: Context,
	private val lifecycleOwner: LifecycleOwner,
) : ServiceConnection {

	private var bindingObserver: BindingLifecycleObserver? = null
	private var collectJob: Job? = null
	private val itemsFlow = MutableStateFlow<List<PausingProgressJob<DownloadState>>>(emptyList())

	val items
		get() = itemsFlow.asFlowLiveData()

	override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
		collectJob?.cancel()
		val binder = (service as? DownloadService.DownloadBinder)
		collectJob = if (binder == null) {
			null
		} else {
			lifecycleOwner.lifecycleScope.launch {
				binder.downloads.collect {
					itemsFlow.value = it
				}
			}
		}
	}

	override fun onServiceDisconnected(name: ComponentName?) {
		collectJob?.cancel()
		collectJob = null
		itemsFlow.value = itemsFlow.value.filter { it.progressValue.isTerminal }
	}

	fun bind() {
		if (bindingObserver != null) {
			return
		}
		bindingObserver = BindingLifecycleObserver().also {
			lifecycleOwner.lifecycle.addObserver(it)
		}
		context.bindService(Intent(context, DownloadService::class.java), this, 0)
	}

	fun unbind() {
		bindingObserver?.let {
			lifecycleOwner.lifecycle.removeObserver(it)
		}
		bindingObserver = null
		context.unbindService(this)
	}

	private inner class BindingLifecycleObserver : DefaultLifecycleObserver {

		override fun onDestroy(owner: LifecycleOwner) {
			super.onDestroy(owner)
			unbind()
		}
	}
}
