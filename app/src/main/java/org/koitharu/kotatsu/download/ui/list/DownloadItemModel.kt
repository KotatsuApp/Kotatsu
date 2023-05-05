package org.koitharu.kotatsu.download.ui.list

import androidx.work.WorkInfo
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.Date
import java.util.UUID

data class DownloadItemModel(
	val id: UUID,
	val workState: WorkInfo.State,
	val manga: Manga,
	val error: Throwable?,
	val max: Int,
	val progress: Int,
	val eta: Long,
	val createdAt: Date,
) : ListModel {

	val percent: Float
		get() = if (max > 0) progress / max.toFloat() else 0f

	val hasEta: Boolean
		get() = eta > 0L
}
