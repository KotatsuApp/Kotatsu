package org.koitharu.kotatsu.details.ui.pager.bookmarks

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
	private val bookmarksRepository: BookmarksRepository,
	settings: AppSettings,
) : BaseViewModel(), FlowCollector<Manga?> {

	private val manga = MutableStateFlow<Manga?>(null)
	val onActionDone = MutableEventFlow<ReversibleAction>()

	val gridScale = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_GRID_SIZE_PAGES,
		valueProducer = { gridSizePages / 100f },
	)

	val content: StateFlow<List<ListModel>> = manga.filterNotNull().flatMapLatest { m ->
		bookmarksRepository.observeBookmarks(m)
			.map { mapList(m, it) }
	}.withErrorHandling()
		.filterNotNull()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, listOf(LoadingState))

	override suspend fun emit(value: Manga?) {
		manga.value = value
	}

	fun removeBookmarks(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			val handle = bookmarksRepository.removeBookmarks(ids)
			onActionDone.call(ReversibleAction(R.string.bookmarks_removed, handle))
		}
	}

	private fun mapList(manga: Manga, bookmarks: List<Bookmark>): List<ListModel>? {
		val chapters = manga.chapters ?: return null
		val bookmarksMap = bookmarks.groupBy { it.chapterId }
		val result = ArrayList<ListModel>(bookmarks.size + bookmarksMap.size)
		for (chapter in chapters) {
			val b = bookmarksMap[chapter.id]
			if (b.isNullOrEmpty()) {
				continue
			}
			result += ListHeader(chapter.name)
			result.addAll(b)
		}
		if (result.isEmpty()) {
			result.add(
				EmptyState(
					icon = 0,
					textPrimary = R.string.no_bookmarks_yet,
					textSecondary = R.string.no_bookmarks_summary,
					actionStringRes = 0,
				),
			)
		}
		return result
	}
}
