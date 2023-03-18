package org.koitharu.kotatsu.local.data.util

import android.os.Build
import android.os.FileObserver
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File

fun File.observe() = callbackFlow {
	val observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		FlowFileObserverQ(this, this@observe)
	} else {
		FlowFileObserver(this, this@observe)
	}
	observer.startWatching()
	awaitClose { observer.stopWatching() }
}.flowOn(Dispatchers.IO)

@RequiresApi(Build.VERSION_CODES.Q)
private class FlowFileObserverQ(
	private val producerScope: ProducerScope<File>,
	private val file: File,
) : FileObserver(file, CREATE or DELETE or CLOSE_WRITE) {

	override fun onEvent(event: Int, path: String?) {
		producerScope.trySendBlocking(
			if (path == null) file else file.resolve(path),
		)
	}
}

@Suppress("DEPRECATION")
private class FlowFileObserver(
	private val producerScope: ProducerScope<File>,
	private val file: File,
) : FileObserver(file.absolutePath, CREATE or DELETE or CLOSE_WRITE) {

	override fun onEvent(event: Int, path: String?) {
		producerScope.trySendBlocking(
			if (path == null) file else file.resolve(path),
		)
	}
}
