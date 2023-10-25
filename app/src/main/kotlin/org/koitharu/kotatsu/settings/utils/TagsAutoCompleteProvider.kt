package org.koitharu.kotatsu.settings.utils

import javax.inject.Inject
import org.koitharu.kotatsu.core.db.MangaDatabase

class TagsAutoCompleteProvider @Inject constructor(
	private val db: MangaDatabase,
) : MultiAutoCompleteTextViewPreference.AutoCompleteProvider {

	override suspend fun getSuggestions(query: String): List<String> {
		if (query.isEmpty()) {
			return emptyList()
		}
		val tags = db.getTagsDao().findTags(query = "$query%", limit = 6)
		val set = HashSet<String>()
		val result = ArrayList<String>(tags.size)
		for (tag in tags) {
			if (set.add(tag.title)) {
				result.add(tag.title)
			}
		}
		return result
	}
}
