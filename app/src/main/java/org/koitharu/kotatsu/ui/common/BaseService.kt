package org.koitharu.kotatsu.ui.common

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

abstract class BaseService : Service(), CoroutineScope {

	private val job = SupervisorJob()

	final override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	@CallSuper
	override fun onDestroy() {
		job.cancel()
		super.onDestroy()
	}

	override fun onBind(intent: Intent?): IBinder? = null
}