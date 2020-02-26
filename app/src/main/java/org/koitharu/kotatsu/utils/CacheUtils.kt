package org.koitharu.kotatsu.utils

import android.content.Context
import androidx.annotation.WorkerThread
import okhttp3.Cache
import okhttp3.CacheControl
import org.koitharu.kotatsu.utils.ext.computeSize
import org.koitharu.kotatsu.utils.ext.sub
import org.koitharu.kotatsu.utils.ext.sumByLong

object CacheUtils {

	@JvmStatic
	val CONTROL_DISABLED = CacheControl.Builder()
		.noCache()
		.noStore()
		.build()

	@JvmStatic
	fun getCacheDirs(context: Context) = (context.externalCacheDirs + context.cacheDir)
		.filterNotNull()
		.distinctBy { it.absolutePath }

	@JvmStatic
	@WorkerThread
	fun computeCacheSize(context: Context, name: String) = getCacheDirs(context)
		.map { it.sub(name) }
		.sumByLong { x -> x.computeSize() }

	@JvmStatic
	@WorkerThread
	fun clearCache(context: Context, name: String) = getCacheDirs(context)
		.map { it.sub(name) }
		.forEach { it.deleteRecursively() }

	@JvmStatic
	fun createHttpCache(context: Context) = Cache(
		directory = (context.externalCacheDir ?: context.cacheDir).sub("http"),
		maxSize = FileSizeUtils.mbToBytes(60)
	)
}