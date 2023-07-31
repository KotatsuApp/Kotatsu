package org.koitharu.kotatsu.search.ui.multi

import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import org.koitharu.kotatsu.parsers.model.MangaSource

class MultiSearchListModel(
	val source: MangaSource,
	val hasMore: Boolean,
	val list: List<MangaItemModel>,
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

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MultiSearchListModel

		if (source != other.source) return false
		if (hasMore != other.hasMore) return false
		if (list != other.list) return false
		return error == other.error
	}

	override fun hashCode(): Int {
		var result = source.hashCode()
		result = 31 * result + hasMore.hashCode()
		result = 31 * result + list.hashCode()
		result = 31 * result + (error?.hashCode() ?: 0)
		return result
	}
}
