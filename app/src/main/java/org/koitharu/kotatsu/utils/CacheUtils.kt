package org.koitharu.kotatsu.utils

import android.content.Context
import android.os.Build
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
	fun getCacheDirs(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
		(context.externalCacheDirs + context.cacheDir)
	} else {
		arrayOf(context.externalCacheDir, context.cacheDir)
	}.filterNotNull()
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
		(context.externalCacheDir ?: context.cacheDir).sub("http"),
		FileSizeUtils.mbToBytes(60)
	)
}