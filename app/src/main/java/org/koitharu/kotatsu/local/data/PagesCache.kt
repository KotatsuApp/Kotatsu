package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.copyToSuspending
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.subdir
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagesCache @Inject constructor(@ApplicationContext context: Context) {

	private val cacheDir = context.externalCacheDir ?: context.cacheDir
	private val lruCache = createDiskLruCacheSafe(
		dir = cacheDir.subdir(CacheDir.PAGES.dir),
		size = FileSize.MEGABYTES.convert(200, FileSize.BYTES),
	)

	operator fun get(url: String): File? {
		return lruCache.get(url)?.takeIfReadable()
	}

	suspend fun put(url: String, inputStream: InputStream): File = withContext(Dispatchers.IO) {
		val file = File(cacheDir, url.longHashCode().toString())
		try {
			file.outputStream().use { out ->
				inputStream.copyToSuspending(out)
			}
			lruCache.put(url, file)
		} finally {
			file.delete()
		}
	}

	suspend fun put(
		url: String,
		inputStream: InputStream,
		contentLength: Long,
		progress: MutableStateFlow<Float>,
	): File = withContext(Dispatchers.IO) {
		val job = currentCoroutineContext()[Job]
		val file = File(cacheDir, url.longHashCode().toString())
		try {
			file.outputStream().use { out ->
				var bytesCopied: Long = 0
				val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
				var bytes = inputStream.read(buffer)
				while (bytes >= 0) {
					out.write(buffer, 0, bytes)
					bytesCopied += bytes
					job?.ensureActive()
					publishProgress(contentLength, bytesCopied, progress)
					bytes = inputStream.read(buffer)
					job?.ensureActive()
				}
			}
			lruCache.put(url, file)
		} finally {
			file.delete()
		}
	}

	private fun publishProgress(contentLength: Long, bytesCopied: Long, progress: MutableStateFlow<Float>) {
		if (contentLength > 0) {
			progress.value = (bytesCopied.toDouble() / contentLength.toDouble()).toFloat()
		}
	}
}

private fun createDiskLruCacheSafe(dir: File, size: Long): DiskLruCache {
	return try {
		DiskLruCache.create(dir, size)
	} catch (e: Exception) {
		dir.deleteRecursively()
		dir.mkdir()
		DiskLruCache.create(dir, size)
	}
}
