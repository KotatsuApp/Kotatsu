package org.koitharu.kotatsu.core.backup

import android.net.Uri
import java.time.LocalDateTime

data class BackupFile(
	val uri: Uri,
	val dateTime: LocalDateTime,
): Comparable<BackupFile> {

	override fun compareTo(other: BackupFile): Int = compareValues(dateTime, other.dateTime)
}
