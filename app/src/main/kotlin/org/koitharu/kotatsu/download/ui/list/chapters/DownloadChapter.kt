package org.koitharu.kotatsu.download.ui.list.chapters

import org.koitharu.kotatsu.list.ui.model.ListModel

data class DownloadChapter(
	val number: Int,
	val name: String,
	val isDownloaded: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is DownloadChapter && other.name == name
	}
}
