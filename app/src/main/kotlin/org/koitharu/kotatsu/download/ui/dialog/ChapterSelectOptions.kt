package org.koitharu.kotatsu.download.ui.dialog

data class ChapterSelectOptions(
	val wholeManga: ChaptersSelectMacro.WholeManga,
	val wholeBranch: ChaptersSelectMacro.WholeBranch?,
	val firstChapters: ChaptersSelectMacro.FirstChapters?,
	val unreadChapters: ChaptersSelectMacro.UnreadChapters?,
)
