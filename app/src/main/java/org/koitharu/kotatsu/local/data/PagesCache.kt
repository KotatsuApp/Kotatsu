package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.copyToSuspending
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.subdir
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import org.koitharu.kotatsu.utils.ext.takeIfWriteable
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagesCache @Inject constructor(@ApplicationContext context: Context) {

	private val cacheDir = checkNotNull(findSuitableDir(context)) {
		val dirs = (context.externalCacheDirs + context.cacheDir).joinToString(";") {
			it?.absolutePath.toString()
		}
		"Cannot find any suitable directory for PagesCache: [$dirs]"
	}
	private val lruCache = createDiskLruCacheSafe(
		dir = cacheDir,
		size = FileSize.MEGABYTES.convert(200, FileSize.BYTES),
	)

	suspend fun get(url: String): File? = runInterruptible(Dispatchers.IO) {
		lruCache.get(url)?.takeIfReadable()
	}

	suspend fun put(url: String, inputStream: InputStream): File = withContext(Dispatchers.IO) {
		val file = File(cacheDir.parentFile, url.longHashCode().toString())
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

private fun findSuitableDir(context: Context): File? {
	val dirs = context.externalCacheDirs + context.cacheDir
	return dirs.firstNotNullOfOrNull {
		it?.subdir(CacheDir.PAGES.dir)?.takeIfWriteable()
	}
}
