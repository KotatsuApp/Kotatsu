package org.koitharu.kotatsu.local.data

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.Source
import okio.buffer
import okio.sink
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.core.util.ext.subdir
import org.koitharu.kotatsu.core.util.ext.takeIfReadable
import org.koitharu.kotatsu.core.util.ext.takeIfWriteable
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.util.ext.printStackTraceDebug
import java.io.File
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

	suspend fun put(url: String, source: Source): File = withContext(Dispatchers.IO) {
		val file = File(cacheDir.get().parentFile, url.longHashCode().toString())
		try {
			val bytes = file.sink(append = false).buffer().use {
				it.writeAllCancellable(source)
			}
			check(bytes != 0L) { "No data has been written" }
			lruCache.get().put(url, file)
		} finally {
			file.delete()
		}
	}
}
