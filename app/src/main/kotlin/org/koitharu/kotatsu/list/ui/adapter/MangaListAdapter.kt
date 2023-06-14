package org.koitharu.kotatsu.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import org.koitharu.kotatsu.list.ui.model.MangaListDetailedModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import kotlin.jvm.internal.Intrinsics

open class MangaListAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager
			.addDelegate(ITEM_TYPE_MANGA_LIST, mangaListItemAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_MANGA_LIST_DETAILED, mangaListDetailedItemAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_MANGA_GRID, mangaGridItemAD(coil, lifecycleOwner, listener, null))
			.addDelegate(ITEM_TYPE_LOADING_FOOTER, loadingFooterAD())
			.addDelegate(ITEM_TYPE_LOADING_STATE, loadingStateAD())
			.addDelegate(ITEM_TYPE_DATE, relatedDateItemAD())
			.addDelegate(ITEM_TYPE_ERROR_STATE, errorStateListAD(listener))
			.addDelegate(ITEM_TYPE_ERROR_FOOTER, errorFooterAD(listener))
			.addDelegate(ITEM_TYPE_EMPTY, emptyStateListAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_HEADER, listHeaderAD(listener))
			.addDelegate(ITEM_TYPE_HEADER_2, listHeader2AD(listener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel) = when {
			oldItem is MangaListModel && newItem is MangaListModel -> {
				oldItem.id == newItem.id
			}

			oldItem is MangaListDetailedModel && newItem is MangaListDetailedModel -> {
				oldItem.id == newItem.id
			}

			oldItem is MangaGridModel && newItem is MangaGridModel -> {
				oldItem.id == newItem.id
			}

			oldItem is DateTimeAgo && newItem is DateTimeAgo -> {
				oldItem == newItem
			}

			oldItem is ListHeader && newItem is ListHeader -> {
				oldItem.textRes == newItem.textRes &&
					oldItem.text == newItem.text &&
					oldItem.dateTimeAgo == newItem.dateTimeAgo
			}

			oldItem is LoadingFooter && newItem is LoadingFooter -> {
				oldItem.key == newItem.key
			}

			else -> oldItem.javaClass == newItem.javaClass
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}

		override fun getChangePayload(oldItem: ListModel, newItem: ListModel): Any? {
			return when (newItem) {
				is MangaItemModel -> {
					oldItem as MangaItemModel
					if (oldItem.progress != newItem.progress) {
						PAYLOAD_PROGRESS
					} else {
					}
				}

				is FilterHeaderModel -> Unit
				else -> super.getChangePayload(oldItem, newItem)
			}
		}
	}

	companion object {

		const val ITEM_TYPE_MANGA_LIST = 0
		const val ITEM_TYPE_MANGA_LIST_DETAILED = 1
		const val ITEM_TYPE_MANGA_GRID = 2
		const val ITEM_TYPE_LOADING_FOOTER = 3
		const val ITEM_TYPE_LOADING_STATE = 4
		const val ITEM_TYPE_DATE = 5
		const val ITEM_TYPE_ERROR_STATE = 6
		const val ITEM_TYPE_ERROR_FOOTER = 7
		const val ITEM_TYPE_EMPTY = 8
		const val ITEM_TYPE_HEADER = 9
		const val ITEM_TYPE_HEADER_2 = 10

		val PAYLOAD_PROGRESS = Any()
	}
}
