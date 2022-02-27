package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.longHashCode
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
}
