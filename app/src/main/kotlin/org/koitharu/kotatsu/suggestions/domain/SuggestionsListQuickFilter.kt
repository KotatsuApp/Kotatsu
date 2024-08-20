package org.koitharu.kotatsu.suggestions.domain

import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.domain.MangaListQuickFilter
import javax.inject.Inject

class SuggestionsListQuickFilter @Inject constructor(
	private val settings: AppSettings,
	private val suggestionRepository: SuggestionRepository,
) : MangaListQuickFilter(settings) {

	override suspend fun getAvailableFilterOptions(): List<ListFilterOption> = buildList(6) {
		suggestionRepository.getTopTags(5).mapTo(this) {
			ListFilterOption.Tag(it)
		}
		if (!settings.isNsfwContentDisabled && !settings.isSuggestionsExcludeNsfw) {
			add(ListFilterOption.Macro.NSFW)
			add(
				ListFilterOption.Inverted(
					option = ListFilterOption.Macro.NSFW,
					iconResId = R.drawable.ic_sfw,
					titleResId = R.string.sfw,
					titleText = null,
				),
			)
		}
	}
}
