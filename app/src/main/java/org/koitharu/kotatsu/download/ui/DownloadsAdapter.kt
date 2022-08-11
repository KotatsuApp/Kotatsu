package org.koitharu.kotatsu.download.ui

import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlinx.coroutines.CoroutineScope
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.utils.progress.PausingProgressJob

typealias DownloadItem = PausingProgressJob<DownloadState>

class DownloadsAdapter(
	scope: CoroutineScope,
	coil: ImageLoader,
) : AsyncListDifferDelegationAdapter<DownloadItem>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(downloadItemAD(scope, coil))
		setHasStableIds(true)
	}

	override fun getItemId(position: Int): Long {
		return items[position].progressValue.startId.toLong()
	}

	private class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {

		override fun areItemsTheSame(
			oldItem: DownloadItem,
			newItem: DownloadItem,
		): Boolean {
			return oldItem.progressValue.startId == newItem.progressValue.startId
		}

		override fun areContentsTheSame(
			oldItem: DownloadItem,
			newItem: DownloadItem,
		): Boolean {
			return oldItem.progressValue == newItem.progressValue
		}
	}
}