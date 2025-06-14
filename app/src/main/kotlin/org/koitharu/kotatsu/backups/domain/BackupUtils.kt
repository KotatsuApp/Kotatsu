package org.koitharu.kotatsu.backups.domain

import android.content.Context
import androidx.annotation.CheckResult
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupUtils {

	private const val DIR_BACKUPS = "backups"
	private val dateTimeFormat = SimpleDateFormat("yyyyMMdd-HHmm")

	@CheckResult
	fun createTempFile(context: Context): File {
		val dir = getAppBackupDir(context)
		dir.mkdirs()
		return File(dir, generateFileName(context))
	}

	fun getAppBackupDir(context: Context) = context.run {
		getExternalFilesDir(DIR_BACKUPS) ?: File(filesDir, DIR_BACKUPS)
	}

	fun parseBackupDateTime(fileName: String): Date? = try {
		dateTimeFormat.parse(fileName.substringAfterLast('_').substringBefore('.'))
	} catch (e: ParseException) {
		e.printStackTraceDebug()
		null
	}

	fun generateFileName(context: Context) = buildString {
		append(context.getString(R.string.app_name).replace(' ', '_').lowercase(Locale.ROOT))
		append('_')
		append(dateTimeFormat.format(Date()))
		append(".bk.zip")
	}
}
