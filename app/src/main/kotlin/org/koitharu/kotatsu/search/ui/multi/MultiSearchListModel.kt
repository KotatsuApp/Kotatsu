package org.koitharu.kotatsu.search.ui.multi

import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import org.koitharu.kotatsu.parsers.model.MangaSource

data class MultiSearchListModel(
	val source: MangaSource,
	val hasMore: Boolean,
	val list: List<MangaListModel>,
	val error: Throwable?,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MultiSearchListModel && source == other.source
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MultiSearchListModel && previousState.list != list) {
			ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
