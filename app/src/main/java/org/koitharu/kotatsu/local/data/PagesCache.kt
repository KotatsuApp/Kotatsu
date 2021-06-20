package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import org.koitharu.kotatsu.utils.FileSizeUtils
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.subdir
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class PagesCache(context: Context) {

	private val cacheDir = context.externalCacheDir ?: context.cacheDir
	private val lruCache = DiskLruCache.create(
		cacheDir.subdir(Cache.PAGES.dir),
		FileSizeUtils.mbToBytes(200)
	)

	operator fun get(url: String): File? {
		return lruCache.get(url)?.takeIfReadable()
	}

	@Deprecated("Useless lambda")
	fun put(url: String, writer: (OutputStream) -> Unit): File {
		val file = File(cacheDir, url.longHashCode().toString())
		file.outputStream().use(writer)
		val res = lruCache.put(url, file)
		file.delete()
		return res
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
}