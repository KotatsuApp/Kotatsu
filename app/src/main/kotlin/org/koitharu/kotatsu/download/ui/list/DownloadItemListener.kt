package org.koitharu.kotatsu.download.ui.list

import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener

interface DownloadItemListener : OnListItemClickListener<DownloadItemModel> {

	fun onCancelClick(item: DownloadItemModel)

	fun onPauseClick(item: DownloadItemModel)

	fun onResumeClick(item: DownloadItemModel, skip: Boolean)

	fun onExpandClick(item: DownloadItemModel)
}
