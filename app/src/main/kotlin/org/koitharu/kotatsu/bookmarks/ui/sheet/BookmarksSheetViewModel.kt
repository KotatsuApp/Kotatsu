package org.koitharu.kotatsu.bookmarks.ui.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import javax.inject.Inject

@HiltViewModel
class BookmarksSheetViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	bookmarksRepository: BookmarksRepository,
) : BaseViewModel() {

	val manga = savedStateHandle.require<ParcelableManga>(BookmarksSheet.ARG_MANGA).manga
	private val chaptersLazy = SuspendLazy {
		requireNotNull(manga.chapters ?: mangaRepositoryFactory.create(manga.source).getDetails(manga).chapters)
	}

	val content: StateFlow<List<ListModel>> = bookmarksRepository.observeBookmarks(manga)
		.map { mapList(it) }
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, listOf(LoadingFooter()))

	private suspend fun mapList(bookmarks: List<Bookmark>): List<ListModel> {
		val chapters = chaptersLazy.get()
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
		return result
	}
}
