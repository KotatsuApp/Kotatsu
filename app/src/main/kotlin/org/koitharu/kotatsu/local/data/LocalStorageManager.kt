package org.koitharu.kotatsu.local.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import androidx.annotation.WorkerThread
import androidx.core.net.toFile
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.Cache
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.computeSize
import org.koitharu.kotatsu.core.util.ext.getStorageName
import org.koitharu.kotatsu.core.util.ext.resolveFile
import java.io.File
import javax.inject.Inject

private const val DIR_NAME = "manga"
private const val NOMEDIA = ".nomedia"
private const val CACHE_DISK_PERCENTAGE = 0.02
private const val CACHE_SIZE_MIN: Long = 10 * 1024 * 1024 // 10MB
private const val CACHE_SIZE_MAX: Long = 250 * 1024 * 1024 // 250MB

@Reusable
class LocalStorageManager @Inject constructor(
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
) {

	val contentResolver: ContentResolver
		get() = context.contentResolver

	@WorkerThread
	fun createHttpCache(): Cache {
		val directory = File(context.externalCacheDir ?: context.cacheDir, "http")
		directory.mkdirs()
		val maxSize = calculateDiskCacheSize(directory)
		return Cache(directory, maxSize)
	}

	suspend fun computeCacheSize(cache: CacheDir) = withContext(Dispatchers.IO) {
		getCacheDirs(cache.dir).sumOf { it.computeSize() }
	}

	suspend fun computeCacheSize() = withContext(Dispatchers.IO) {
		getCacheDirs().sumOf { it.computeSize() }
	}

	suspend fun computeStorageSize() = withContext(Dispatchers.IO) {
		getAvailableStorageDirs().sumOf { it.computeSize() }
	}

	suspend fun computeAvailableSize() = runInterruptible(Dispatchers.IO) {
		getAvailableStorageDirs().sumOf { it.freeSpace }
	}

	suspend fun clearCache(cache: CacheDir) = runInterruptible(Dispatchers.IO) {
		getCacheDirs(cache.dir).forEach { it.deleteRecursively() }
	}

	suspend fun getReadableDirs(): List<File> = runInterruptible(Dispatchers.IO) {
		getConfiguredStorageDirs()
			.filter { it.isReadable() }
	}

	suspend fun getWriteableDirs(): List<File> = runInterruptible(Dispatchers.IO) {
		getConfiguredStorageDirs()
			.filter { it.isWriteable() }
	}

	suspend fun getDefaultWriteableDir(): File? = runInterruptible(Dispatchers.IO) {
		val preferredDir = settings.mangaStorageDir?.takeIf { it.isWriteable() }
		preferredDir ?: getFallbackStorageDir()?.takeIf { it.isWriteable() }
	}

	suspend fun getApplicationStorageDirs(): Set<File> = runInterruptible(Dispatchers.IO) {
		getAvailableStorageDirs()
	}

	suspend fun resolveUri(uri: Uri): File? = runInterruptible(Dispatchers.IO) {
		if (uri.scheme == "file") {
			uri.toFile()
		} else {
			uri.resolveFile(context)
		}
	}

	suspend fun setDirIsNoMedia(dir: File) = runInterruptible(Dispatchers.IO) {
		File(dir, NOMEDIA).createNewFile()
	}

	fun takePermissions(uri: Uri) {
		val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
		contentResolver.takePersistableUriPermission(uri, flags)
	}

	suspend fun getDirectoryDisplayName(dir: File, isFullPath: Boolean): String = runInterruptible(Dispatchers.IO) {
		val packageName = context.packageName
		if (dir.absolutePath.contains(packageName)) {
			dir.getStorageName(context)
		} else if (isFullPath) {
			dir.path
		} else {
			dir.name
		}
	}

	@WorkerThread
	private fun getConfiguredStorageDirs(): Set<File> {
		return getAvailableStorageDirs() + settings.userSpecifiedMangaDirectories
	}

	@WorkerThread
	private fun getAvailableStorageDirs(): Set<File> {
		return (sequenceOf(File(context.filesDir, DIR_NAME)) + context.getExternalFilesDirs(DIR_NAME))
			.filterNotNull()
			.filter { it.exists() || it.mkdirs() }
			.toSet()
	}

	@WorkerThread
	private fun getFallbackStorageDir(): File? {
		return context.getExternalFilesDir(DIR_NAME) ?: File(context.filesDir, DIR_NAME).takeIf {
			it.exists() || it.mkdirs()
		}
	}

	@WorkerThread
	private fun getCacheDirs(subDir: String = ""): Set<File> {
		return (sequenceOf(context.cacheDir) + context.externalCacheDirs)
			.filterNotNull()
			.map { File(it, subDir) }
			.toSet()
	}

	private fun calculateDiskCacheSize(cacheDirectory: File): Long {
		return try {
			val cacheDir = StatFs(cacheDirectory.absolutePath)
			val size = CACHE_DISK_PERCENTAGE * cacheDir.blockCountLong * cacheDir.blockSizeLong
			return size.toLong().coerceIn(CACHE_SIZE_MIN, CACHE_SIZE_MAX)
		} catch (_: Exception) {
			CACHE_SIZE_MIN
		}
	}

	private fun File.isReadable() = runCatching {
		canRead()
	}.getOrDefault(false)

	private fun File.isWriteable() = runCatching {
		canWrite()
	}.getOrDefault(false)
}
