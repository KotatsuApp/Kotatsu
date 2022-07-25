package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.subdir
import org.koitharu.kotatsu.utils.ext.takeIfReadable

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

	fun put(url: String, inputStream: InputStream): File {
		val file = File(cacheDir, url.longHashCode().toString())
		file.outputStream().use { out ->
			inputStream.copyTo(out)
		}
		val res = lruCache.put(url, file)
		file.delete()
		return res
	}

	fun put(
		url: String,
		inputStream: InputStream,
		contentLength: Long,
		progress: MutableStateFlow<Float>,
	): File {
		val file = File(cacheDir, url.longHashCode().toString())
		file.outputStream().use { out ->
			var bytesCopied: Long = 0
			val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
			var bytes = inputStream.read(buffer)
			while (bytes >= 0) {
				out.write(buffer, 0, bytes)
				bytesCopied += bytes
				publishProgress(contentLength, bytesCopied, progress)
				bytes = inputStream.read(buffer)
			}
		}
		val res = lruCache.put(url, file)
		file.delete()
		return res
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
