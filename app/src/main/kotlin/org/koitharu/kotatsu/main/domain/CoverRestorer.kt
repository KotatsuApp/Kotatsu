package org.koitharu.kotatsu.main.domain

import androidx.lifecycle.coroutineScope
import coil.EventListener
import coil.ImageLoader
import coil.network.HttpException
import coil.request.ErrorResult
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.jsoup.HttpStatusException
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Provider

class CoverRestorer @Inject constructor(
	private val dataRepository: MangaDataRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val repositoryFactory: MangaRepository.Factory,
	private val coilProvider: Provider<ImageLoader>,
) : EventListener {

	override fun onError(request: ImageRequest, result: ErrorResult) {
		super.onError(request, result)
		if (!result.throwable.shouldRestore()) {
			return
		}
		request.tags.tag<Manga>()?.let {
			restoreManga(it, request)
		}
		request.tags.tag<Bookmark>()?.let {
			restoreBookmark(it, request)
		}
	}

	private fun restoreManga(manga: Manga, request: ImageRequest) {
		request.lifecycle.coroutineScope.launch {
			val restored = runCatchingCancellable {
				restoreMangaImpl(manga)
			}.getOrDefault(false)
			if (restored) {
				request.newBuilder().enqueueWith(coilProvider.get())
			}
		}
	}

	private suspend fun restoreMangaImpl(manga: Manga): Boolean {
		if (dataRepository.findMangaById(manga.id) == null) {
			return false
		}
		val repo = repositoryFactory.create(manga.source) as? RemoteMangaRepository ?: return false
		val fixed = repo.find(manga) ?: return false
		return if (fixed != manga) {
			dataRepository.storeManga(fixed)
			fixed.coverUrl != manga.coverUrl
		} else {
			false
		}
	}

	private fun restoreBookmark(bookmark: Bookmark, request: ImageRequest) {
		request.lifecycle.coroutineScope.launch {
			val restored = runCatchingCancellable {
				restoreBookmarkImpl(bookmark)
			}.getOrDefault(false)
			if (restored) {
				request.newBuilder().enqueueWith(coilProvider.get())
			}
		}
	}

	private suspend fun restoreBookmarkImpl(bookmark: Bookmark): Boolean {
		val repo = repositoryFactory.create(bookmark.manga.source) as? RemoteMangaRepository ?: return false
		val chapter = repo.getDetails(bookmark.manga).chapters?.find { it.id == bookmark.chapterId } ?: return false
		val page = repo.getPages(chapter)[bookmark.page]
		val imageUrl = page.preview.ifNullOrEmpty { page.url }
		return if (imageUrl != bookmark.imageUrl) {
			bookmarksRepository.updateBookmark(bookmark, imageUrl)
			true
		} else {
			false
		}
	}

	private fun Throwable.shouldRestore(): Boolean {
		return this is HttpException || this is HttpStatusException || this is ParseException
	}
}
