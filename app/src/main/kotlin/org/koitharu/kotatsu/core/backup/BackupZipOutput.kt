package org.koitharu.kotatsu.core.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.Closeable
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.zip.ZipOutput
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater

class BackupZipOutput(val file: File) : Closeable {

	private val output = ZipOutput(file, Deflater.BEST_COMPRESSION)

	suspend fun put(entry: BackupEntry) = runInterruptible(Dispatchers.IO) {
		output.put(entry.name.key, entry.data.toString(2))
	}

	suspend fun finish() = runInterruptible(Dispatchers.IO) {
		output.finish()
	}

	override fun close() {
		output.close()
	}

	companion object {

		const val DIR_BACKUPS = "backups"
		private val dateTimeFormat = SimpleDateFormat("yyyyMMdd-HHmm")

		fun generateFileName(context: Context) = buildString {
			append(context.getString(R.string.app_name).replace(' ', '_').lowercase(Locale.ROOT))
			append('_')
			append(dateTimeFormat.format(Date()))
			append(".bk.zip")
		}

		fun parseBackupDateTime(fileName: String): Date? = try {
			dateTimeFormat.parse(fileName.substringAfterLast('_').substringBefore('.'))
		} catch (e: ParseException) {
			e.printStackTraceDebug()
			null
		}

		suspend fun createTemp(context: Context): BackupZipOutput = runInterruptible(Dispatchers.IO) {
			val dir = context.run {
				getExternalFilesDir(DIR_BACKUPS) ?: File(filesDir, DIR_BACKUPS)
			}
			dir.mkdirs()
			BackupZipOutput(File(dir, generateFileName(context)))
		}
	}
}
