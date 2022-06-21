package org.koitharu.kotatsu.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LifecycleAwareServiceConnection(
	private val host: Activity,
) : ServiceConnection, DefaultLifecycleObserver {

	private val serviceStateFlow = MutableStateFlow<IBinder?>(null)

	val service: StateFlow<IBinder?>
		get() = serviceStateFlow

	override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
		serviceStateFlow.value = service
	}

	override fun onServiceDisconnected(name: ComponentName?) {
		serviceStateFlow.value = null
	}

	override fun onDestroy(owner: LifecycleOwner) {
		super.onDestroy(owner)
		host.unbindService(this)
	}
}

fun Activity.bindServiceWithLifecycle(
	owner: LifecycleOwner,
	service: Intent,
	flags: Int
): LifecycleAwareServiceConnection {
	val connection = LifecycleAwareServiceConnection(this)
	bindService(service, connection, flags)
	owner.lifecycle.addObserver(connection)
	return connection
}