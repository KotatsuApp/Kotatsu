package org.koitharu.kotatsu.core.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.Closeable
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.format
import org.koitharu.kotatsu.core.zip.ZipOutput
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater

class BackupZipOutput(val file: File) : Closeable {

	private val output = ZipOutput(file, Deflater.BEST_COMPRESSION)

	suspend fun put(entry: BackupEntry) = runInterruptible(Dispatchers.IO) {
		output.put(entry.name, entry.data.toString(2))
	}

	suspend fun finish() = runInterruptible(Dispatchers.IO) {
		output.finish()
	}

	override fun close() {
		output.close()
	}
}

private const val DIR_BACKUPS = "backups"

suspend fun BackupZipOutput(context: Context): BackupZipOutput = runInterruptible(Dispatchers.IO) {
	val dir = context.run {
		getExternalFilesDir(DIR_BACKUPS) ?: File(filesDir, DIR_BACKUPS)
	}
	dir.mkdirs()
	val filename = buildString {
		append(context.getString(R.string.app_name).replace(' ', '_').lowercase(Locale.ROOT))
		append('_')
		append(Date().format("ddMMyyyy"))
		append(".bk.zip")
	}
	BackupZipOutput(File(dir, filename))
}
