package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.copyToSuspending
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import org.koitharu.kotatsu.utils.ext.subdir
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import org.koitharu.kotatsu.utils.ext.takeIfWriteable
import java.io.File
import java.io.InputStream
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
		val size = FileSize.MEGABYTES.convert(200, FileSize.BYTES)
		runCatchingCancellable {
			DiskLruCache.create(dir, size)
		}.recoverCatching { error ->
			error.printStackTraceDebug()
			dir.deleteRecursively()
			dir.mkdir()
			DiskLruCache.create(dir, size)
		}.getOrThrow()
	}

	suspend fun get(url: String): File? {
		val cache = lruCache.get()
		return runInterruptible(Dispatchers.IO) {
			cache.get(url)?.takeIfReadable()
		}
	}

	suspend fun put(url: String, inputStream: InputStream): File = withContext(Dispatchers.IO) {
		val file = File(cacheDir.get().parentFile, url.longHashCode().toString())
		try {
			file.outputStream().use { out ->
				inputStream.copyToSuspending(out)
			}
			lruCache.get().put(url, file)
		} finally {
			file.delete()
		}
	}
}
