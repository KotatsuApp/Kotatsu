package org.koitharu.kotatsu.download.ui.worker

import android.os.Parcelable
import androidx.work.Data
import kotlinx.parcelize.Parcelize
import org.koitharu.kotatsu.core.prefs.DownloadFormat
import org.koitharu.kotatsu.parsers.util.find
import java.io.File

@Parcelize
class DownloadTask(
	val mangaId: Long,
	val isPaused: Boolean,
	val isSilent: Boolean,
	val chaptersIds: LongArray?,
	val destination: File?,
	val format: DownloadFormat?,
	val allowMeteredNetwork: Boolean,
) : Parcelable {

	constructor(data: Data) : this(
		mangaId = data.getLong(MANGA_ID, 0L),
		isPaused = data.getBoolean(START_PAUSED, false),
		isSilent = data.getBoolean(IS_SILENT, false),
		chaptersIds = data.getLongArray(CHAPTERS)?.takeUnless(LongArray::isEmpty),
		destination = data.getString(DESTINATION)?.let { File(it) },
		format = data.getString(FORMAT)?.let { DownloadFormat.entries.find(it) },
		allowMeteredNetwork = data.getBoolean(ALLOW_METERED, true),
	)

	fun toData(): Data = Data.Builder()
		.putLong(MANGA_ID, mangaId)
		.putBoolean(START_PAUSED, isPaused)
		.putBoolean(IS_SILENT, isSilent)
		.putLongArray(CHAPTERS, chaptersIds ?: LongArray(0))
		.putString(DESTINATION, destination?.path)
		.putString(FORMAT, format?.name)
		.build()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as DownloadTask

		if (mangaId != other.mangaId) return false
		if (isPaused != other.isPaused) return false
		if (isSilent != other.isSilent) return false
		if (!(chaptersIds contentEquals other.chaptersIds)) return false
		if (destination != other.destination) return false
		if (format != other.format) return false
		if (allowMeteredNetwork != other.allowMeteredNetwork) return false

		return true
	}

	override fun hashCode(): Int {
		var result = mangaId.hashCode()
		result = 31 * result + isPaused.hashCode()
		result = 31 * result + isSilent.hashCode()
		result = 31 * result + (chaptersIds?.contentHashCode() ?: 0)
		result = 31 * result + (destination?.hashCode() ?: 0)
		result = 31 * result + (format?.hashCode() ?: 0)
		result = 31 * result + allowMeteredNetwork.hashCode()
		return result
	}

	private companion object {

		const val MANGA_ID = "manga_id"
		const val IS_SILENT = "silent"
		const val START_PAUSED = "paused"
		const val CHAPTERS = "chapters"
		const val DESTINATION = "dest"
		const val FORMAT = "format"
		const val ALLOW_METERED = "metered"
	}
}
