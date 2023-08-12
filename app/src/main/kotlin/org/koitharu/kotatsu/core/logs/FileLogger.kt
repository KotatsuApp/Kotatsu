package org.koitharu.kotatsu.core.logs

import android.content.Context
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.core.util.ext.subdir
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

private const val DIR = "logs"
private const val FLUSH_DELAY = 2_000L
private const val MAX_SIZE_BYTES = 1024 * 1024 // 1 MB

class FileLogger(
	context: Context,
	private val settings: AppSettings,
	name: String,
) {

	val file by lazy {
		val dir = context.getExternalFilesDir(DIR) ?: context.filesDir.subdir(DIR)
		File(dir, "$name.log")
	}
	val isEnabled: Boolean
		get() = settings.isLoggingEnabled
	private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.ROOT)
	private val buffer = ConcurrentLinkedQueue<String>()
	private val mutex = Mutex()
	private var flushJob: Job? = null

	fun log(message: String, e: Throwable? = null) {
		if (!isEnabled) {
			return
		}
		val text = buildString {
			append(dateTimeFormatter.format(LocalDateTime.now()))
			append(": ")
			if (e != null) {
				append("E!")
			}
			append(message)
			if (e != null) {
				append(' ')
				append(e.stackTraceToString())
				appendLine()
			}
		}
		buffer.add(text)
		postFlush()
	}

	inline fun log(messageProducer: () -> String) {
		if (isEnabled) {
			log(messageProducer())
		}
	}

	suspend fun flush() {
		if (!isEnabled) {
			return
		}
		flushJob?.cancelAndJoin()
		flushImpl()
	}

	@WorkerThread
	fun flushBlocking() {
		if (!isEnabled) {
			return
		}
		runBlockingSafe { flushJob?.cancelAndJoin() }
		runBlockingSafe { flushImpl() }
	}

	private fun postFlush() {
		if (flushJob?.isActive == true) {
			return
		}
		flushJob = processLifecycleScope.launch(Dispatchers.Default) {
			delay(FLUSH_DELAY)
			runCatchingCancellable {
				flushImpl()
			}.onFailure {
				it.printStackTraceDebug()
			}
		}
	}

	private suspend fun flushImpl() = withContext(NonCancellable) {
		mutex.withLock {
			if (buffer.isEmpty()) {
				return@withContext
			}
			runInterruptible(Dispatchers.IO) {
				if (file.length() > MAX_SIZE_BYTES) {
					rotate()
				}
				FileOutputStream(file, true).use {
					while (true) {
						val message = buffer.poll() ?: break
						it.write(message.toByteArray())
						it.write('\n'.code)
					}
					it.flush()
				}
			}
		}
	}

	@WorkerThread
	private fun rotate() {
		val length = file.length()
		val bakFile = File(file.parentFile, file.name + ".bak")
		file.renameTo(bakFile)
		bakFile.inputStream().use { input ->
			input.skip(length - MAX_SIZE_BYTES / 2)
			file.outputStream().use { output ->
				input.copyTo(output)
				output.flush()
			}
		}
		bakFile.delete()
	}

	private inline fun runBlockingSafe(crossinline block: suspend () -> Unit) = try {
		runBlocking(NonCancellable) { block() }
	} catch (_: InterruptedException) {
	}
}
