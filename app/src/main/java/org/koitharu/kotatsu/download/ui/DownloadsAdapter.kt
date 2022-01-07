package org.koitharu.kotatsu.download.ui

import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlinx.coroutines.CoroutineScope
import org.koitharu.kotatsu.download.domain.DownloadManager
import org.koitharu.kotatsu.utils.JobStateFlow

class DownloadsAdapter(
	scope: CoroutineScope,
	coil: ImageLoader,
) : AsyncListDifferDelegationAdapter<JobStateFlow<DownloadManager.State>>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(downloadItemAD(scope, coil))
		setHasStableIds(true)
	}

	override fun getItemId(position: Int): Long {
		return items[position].value.startId.toLong()
	}

	private class DiffCallback : DiffUtil.ItemCallback<JobStateFlow<DownloadManager.State>>() {

		override fun areItemsTheSame(
			oldItem: JobStateFlow<DownloadManager.State>,
			newItem: JobStateFlow<DownloadManager.State>,
		): Boolean {
			return oldItem.value.startId == newItem.value.startId
		}

		override fun areContentsTheSame(
			oldItem: JobStateFlow<DownloadManager.State>,
			newItem: JobStateFlow<DownloadManager.State>,
		): Boolean {
			return oldItem.value == newItem.value
		}
	}
}