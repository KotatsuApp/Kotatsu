package org.koitharu.kotatsu.utils

import android.content.Context
import android.os.StatFs
import androidx.annotation.WorkerThread
import okhttp3.Cache
import okhttp3.CacheControl
import org.koitharu.kotatsu.utils.ext.computeSize
import org.koitharu.kotatsu.utils.ext.sub
import org.koitharu.kotatsu.utils.ext.sumByLong
import java.io.File

object CacheUtils {

	val CONTROL_DISABLED = CacheControl.Builder()
		.noStore()
		.build()

	fun getCacheDirs(context: Context) = (context.externalCacheDirs + context.cacheDir)
		.filterNotNull()
		.distinctBy { it.absolutePath }

	@WorkerThread
	fun computeCacheSize(context: Context, name: String) = getCacheDirs(context)
		.map { it.sub(name) }
		.sumByLong { x -> x.computeSize() }

	@WorkerThread
	fun clearCache(context: Context, name: String) = getCacheDirs(context)
		.map { it.sub(name) }
		.forEach { it.deleteRecursively() }

	fun createHttpCache(context: Context): Cache {
		val directory = (context.externalCacheDir ?: context.cacheDir).sub("http")
		directory.mkdirs()
		val maxSize = calculateDiskCacheSize(directory) // TODO blocking call
		return Cache(directory, maxSize)
	}

	private fun calculateDiskCacheSize(cacheDirectory: File): Long {
		return try {
			val cacheDir = StatFs(cacheDirectory.absolutePath)
			val size = DISK_CACHE_PERCENTAGE * cacheDir.blockCountLong * cacheDir.blockSizeLong
			return size.toLong().coerceIn(MIN_DISK_CACHE_SIZE, MAX_DISK_CACHE_SIZE)
		} catch (_: Exception) {
			MIN_DISK_CACHE_SIZE
		}
	}

	private const val DISK_CACHE_PERCENTAGE = 0.02
	private const val MIN_DISK_CACHE_SIZE: Long = 10 * 1024 * 1024 // 10MB
	private const val MAX_DISK_CACHE_SIZE: Long = 250 * 1024 * 1024 // 250MB
}