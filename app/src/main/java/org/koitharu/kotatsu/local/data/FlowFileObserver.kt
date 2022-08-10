package org.koitharu.kotatsu.local.data

import android.os.FileObserver
import java.io.File
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

@Suppress("DEPRECATION")
class FlowFileObserver(
	private val producerScope: ProducerScope<File>,
	private val file: File,
) : FileObserver(file.absolutePath, CREATE or DELETE or CLOSE_WRITE) {

	override fun onEvent(event: Int, path: String?) {
		producerScope.trySendBlocking(
			if (path == null) file else file.resolve(path),
		)
	}
}

fun File.observe() = callbackFlow {
	val observer = FlowFileObserver(this, this@observe)
	observer.startWatching()
	awaitClose { observer.stopWatching() }
}
