package org.koitharu.kotatsu.utils

import android.content.Context
import androidx.annotation.WorkerThread
import org.koitharu.kotatsu.utils.ext.computeSize
import org.koitharu.kotatsu.utils.ext.sub
import org.koitharu.kotatsu.utils.ext.sumByLong

object CacheUtils {

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
}