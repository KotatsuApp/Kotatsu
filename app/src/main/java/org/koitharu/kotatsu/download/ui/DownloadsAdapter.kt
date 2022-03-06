package org.koitharu.kotatsu.download.ui

import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlinx.coroutines.CoroutineScope
import org.koitharu.kotatsu.download.domain.DownloadManager
import org.koitharu.kotatsu.utils.progress.ProgressJob

class DownloadsAdapter(
	scope: CoroutineScope,
	coil: ImageLoader,
) : AsyncListDifferDelegationAdapter<ProgressJob<DownloadManager.State>>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(downloadItemAD(scope, coil))
		setHasStableIds(true)
	}

	override fun getItemId(position: Int): Long {
		return items[position].progressValue.startId.toLong()
	}

	private class DiffCallback : DiffUtil.ItemCallback<ProgressJob<DownloadManager.State>>() {

		override fun areItemsTheSame(
			oldItem: ProgressJob<DownloadManager.State>,
			newItem: ProgressJob<DownloadManager.State>,
		): Boolean {
			return oldItem.progressValue.startId == newItem.progressValue.startId
		}

		override fun areContentsTheSame(
			oldItem: ProgressJob<DownloadManager.State>,
			newItem: ProgressJob<DownloadManager.State>,
		): Boolean {
			return oldItem.progressValue == newItem.progressValue
		}
	}
}