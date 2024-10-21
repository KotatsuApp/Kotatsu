package org.koitharu.kotatsu.local.data

import android.content.Context
import android.graphics.Bitmap
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.Source
import okio.buffer
import okio.sink
import okio.use
import org.koitharu.kotatsu.core.exceptions.NoDataReceivedException
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.compressToPNG
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.subdir
import org.koitharu.kotatsu.core.util.ext.takeIfReadable
import org.koitharu.kotatsu.core.util.ext.takeIfWriteable
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagesCache @Inject constructor(@ApplicationContext context: Context) {

	private val cacheDir = SuspendLazy {
		val dirs = context.externalCacheDirs + context.cacheDir
		dirs.firstNotNullOf {
			it?.subdir(CacheDir.PAGES.dir)?.takeIfWriteable()
		}
	}
	private val lruCache = SuspendLazy {
		val dir = cacheDir.get()
		val availableSize = (getAvailableSize() * 0.8).toLong()
		val size = SIZE_DEFAULT.coerceAtMost(availableSize).coerceAtLeast(SIZE_MIN)
		runCatchingCancellable {
			DiskLruCache.create(dir, size)
		}.recoverCatching { error ->
			error.printStackTraceDebug()
			dir.deleteRecursively()
			dir.mkdir()
			DiskLruCache.create(dir, size)
		}.getOrThrow()
	}

	suspend fun get(url: String): File? = withContext(Dispatchers.IO) {
		val cache = lruCache.get()
		runInterruptible {
			cache.get(url)?.takeIfReadable()
		}
	}

	suspend fun put(url: String, source: Source, mimeType: String?): File = withContext(Dispatchers.IO) {
		val file = createBufferFile(url, mimeType)
		try {
			val bytes = file.sink(append = false).buffer().use {
				it.writeAllCancellable(source)
			}
			if (bytes == 0L) {
				throw NoDataReceivedException(url)
			}
			val cache = lruCache.get()
			runInterruptible {
				cache.put(url, file)
			}
		} finally {
			file.delete()
		}
	}

	suspend fun put(url: String, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
		val file = createBufferFile(url, "image/png")
		try {
			bitmap.compressToPNG(file)
			val cache = lruCache.get()
			runInterruptible {
				cache.put(url, file)
			}
		} finally {
			file.delete()
		}
	}

	suspend fun clear() {
		val cache = lruCache.get()
		runInterruptible(Dispatchers.IO) {
			cache.clearCache()
		}
	}

	private suspend fun getAvailableSize(): Long = runCatchingCancellable {
		val dir = cacheDir.get()
		runInterruptible(Dispatchers.IO) {
			val statFs = StatFs(dir.absolutePath)
			statFs.availableBytes
		}
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrDefault(SIZE_DEFAULT)

	private suspend fun createBufferFile(url: String, mimeType: String?): File {
		val ext = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
			?: MimeTypeMap.getFileExtensionFromUrl(url).ifNullOrEmpty { "dat" }
		val cacheDir = cacheDir.get()
		val rootDir = checkNotNull(cacheDir.parentFile) { "Cannot get parent for ${cacheDir.absolutePath}" }
		val name = UUID.randomUUID().toString() + "." + ext
		return File(rootDir, name)
	}

	private companion object {

		val SIZE_MIN
			get() = FileSize.MEGABYTES.convert(20, FileSize.BYTES)

		val SIZE_DEFAULT
			get() = FileSize.MEGABYTES.convert(200, FileSize.BYTES)
	}
}
