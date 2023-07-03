package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.parsers.model.Manga

class MangaListDetailedModel(
	override val id: Long,
	override val title: String,
	val subtitle: String?,
	override val coverUrl: String,
	override val manga: Manga,
	override val counter: Int,
	override val progress: Float,
	val tags: List<ChipsView.ChipModel>,
) : MangaItemModel() {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MangaListDetailedModel

		if (id != other.id) return false
		if (title != other.title) return false
		if (subtitle != other.subtitle) return false
		if (coverUrl != other.coverUrl) return false
		if (manga != other.manga) return false
		if (counter != other.counter) return false
		if (progress != other.progress) return false
		return tags == other.tags
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + (subtitle?.hashCode() ?: 0)
		result = 31 * result + coverUrl.hashCode()
		result = 31 * result + manga.hashCode()
		result = 31 * result + counter
		result = 31 * result + progress.hashCode()
		result = 31 * result + tags.hashCode()
		return result
	}
}
