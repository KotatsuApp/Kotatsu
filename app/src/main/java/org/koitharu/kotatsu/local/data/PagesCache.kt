package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
