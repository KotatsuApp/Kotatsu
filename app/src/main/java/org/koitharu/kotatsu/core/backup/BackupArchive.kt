package org.koitharu.kotatsu.core.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.MutableZipFile
import org.koitharu.kotatsu.utils.ext.format
import java.io.File
import java.util.*

class BackupArchive(file: File) : MutableZipFile(file) {

	init {
		if (!dir.exists()) {
			dir.mkdirs()
		}
	}

	suspend fun put(entry: BackupEntry) {
		put(entry.name, entry.data.toString(2))
	}

	suspend fun getEntry(name: String): BackupEntry {
		val json = withContext(Dispatchers.Default) {
			JSONArray(getContent(name))
		}
		return BackupEntry(name, json)
	}

	companion object {

		private const val DIR_BACKUPS = "backups"

		@Suppress("BlockingMethodInNonBlockingContext")
		suspend fun createNew(context: Context): BackupArchive = withContext(Dispatchers.IO) {
			val dir = context.run {
				getExternalFilesDir(DIR_BACKUPS) ?: File(filesDir, DIR_BACKUPS)
			}
			dir.mkdirs()
			val filename = buildString {
				append(context.getString(R.string.app_name).toLowerCase(Locale.ROOT))
				append('_')
				append(Date().format("ddMMyyyy"))
				append(".bak")
			}
			BackupArchive(File(dir, filename))
		}
	}
}