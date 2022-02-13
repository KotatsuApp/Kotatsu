package org.koitharu.kotatsu.local.data

import android.content.Context
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.ext.getStorageName
import java.io.File

private const val DIR_NAME = "manga"

class LocalStorageManager(
	private val context: Context,
	private val settings: AppSettings,
) {

	suspend fun getReadableDirs(): List<File> = runInterruptible(Dispatchers.IO) {
		getConfiguredStorageDirs()
			.filter { it.isReadable() }
	}

	suspend fun getWriteableDirs(): List<File> = runInterruptible(Dispatchers.IO) {
		getConfiguredStorageDirs()
			.filter { it.isWriteable() }
	}

	suspend fun getDefaultWriteableDir(): File? = runInterruptible(Dispatchers.IO) {
		val preferredDir = settings.getFallbackStorageDir()?.takeIf { it.isWriteable() }
		preferredDir ?: getFallbackStorageDir()?.takeIf { it.isWriteable() }
	}

	fun getStorageDisplayName(file: File) = file.getStorageName(context)

	@WorkerThread
	private fun getConfiguredStorageDirs(): MutableSet<File> {
		val set = getAvailableStorageDirs()
		settings.getFallbackStorageDir()?.let {
			set.add(it)
		}
		return set
	}

	@WorkerThread
	private fun getAvailableStorageDirs(): MutableSet<File> {
		val result = LinkedHashSet<File>()
		result += File(context.filesDir, DIR_NAME)
		result += context.getExternalFilesDirs(DIR_NAME)
		result.retainAll { it.exists() || it.mkdirs() }
		return result
	}

	@WorkerThread
	private fun getFallbackStorageDir(): File? {
		return context.getExternalFilesDir(DIR_NAME) ?: File(context.filesDir, DIR_NAME).takeIf {
			it.exists() || it.mkdirs()
		}
	}

	private fun File.isReadable() = runCatching {
		canRead()
	}.getOrDefault(false)

	private fun File.isWriteable() = runCatching {
		canWrite()
	}.getOrDefault(false)
}