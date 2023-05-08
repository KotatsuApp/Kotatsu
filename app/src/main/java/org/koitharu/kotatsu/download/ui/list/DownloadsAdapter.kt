package org.koitharu.kotatsu.download.ui.list

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.adapter.relatedDateItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import kotlin.jvm.internal.Intrinsics

class DownloadsAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: DownloadItemListener,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(ITEM_TYPE_DOWNLOAD, downloadItemAD(lifecycleOwner, coil, listener))
			.addDelegate(loadingStateAD())
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, null))
			.addDelegate(relatedDateItemAD())
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel) = when {

			oldItem is DownloadItemModel && newItem is DownloadItemModel -> {
				oldItem.id == newItem.id
			}

			oldItem is DateTimeAgo && newItem is DateTimeAgo -> {
				oldItem == newItem
			}

			else -> oldItem.javaClass == newItem.javaClass
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}

		override fun getChangePayload(oldItem: ListModel, newItem: ListModel): Any? {
			return when (newItem) {
				is DownloadItemModel -> {
					oldItem as DownloadItemModel
					if (oldItem.workState == newItem.workState) {
						Unit
					} else {
						null
					}
				}

				else -> super.getChangePayload(oldItem, newItem)
			}
		}
	}

	companion object {
		const val ITEM_TYPE_DOWNLOAD = 0
	}
}
