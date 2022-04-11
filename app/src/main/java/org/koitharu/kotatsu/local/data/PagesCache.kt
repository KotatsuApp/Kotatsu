package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.parsers.util.longHashCode
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.subdir
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import java.io.File
import java.io.InputStream

class PagesCache(context: Context) {

	private val cacheDir = context.externalCacheDir ?: context.cacheDir
	private val lruCache = DiskLruCache.create(
		cacheDir.subdir(CacheDir.PAGES.dir),
		FileSize.MEGABYTES.convert(200, FileSize.BYTES),
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
