package org.koitharu.kotatsu.ui.base

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class BaseService : Service() {

	@Suppress("MemberVisibilityCanBePrivate")
	val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

	@CallSuper
	override fun onDestroy() {
		serviceScope.cancel()
		super.onDestroy()
	}

	override fun onBind(intent: Intent?): IBinder? = null
}