package org.koitharu.kotatsu.ui.reader

import android.content.ContentResolver
import android.webkit.URLUtil
import kotlinx.coroutines.*
import moxy.InjectViewState
import moxy.presenterScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.get
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.domain.MangaDataRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.MangaUtils
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.utils.MediaStoreCompat
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.contentDisposition
import org.koitharu.kotatsu.utils.ext.mimeType

@InjectViewState
class ReaderPresenter : BasePresenter<ReaderView>() {

	fun init(manga: Manga) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(isLoading = true)
			try {
				val mode = withContext(Dispatchers.IO) {
					val repo = MangaProviderFactory.create(manga.source)
					val chapter =
						(manga.chapters ?: throw RuntimeException("Chapters is null")).random()
					val prefs = MangaDataRepository()
					var mode = prefs.getReaderMode(manga.id)
					if (mode == null) {
						val pages = repo.getPages(chapter)
						mode = MangaUtils.determineReaderMode(pages)
						if (mode != null) {
							prefs.savePreferences(
								manga = manga,
								mode = mode
							)
						}
					}
					mode ?: ReaderMode.UNKNOWN
				}
				viewState.onInitReader(manga, mode)
			} catch (_: CancellationException) {
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(isLoading = false)
			}
		}
	}

	fun setMode(manga: Manga, mode: ReaderMode) {
		presenterScope.launch(Dispatchers.IO) {
			MangaDataRepository().savePreferences(
				manga = manga,
				mode = mode
			)
		}
		viewState.onInitReader(manga, mode)
	}

	fun savePage(resolver: ContentResolver, page: MangaPage) {
		presenterScope.launch(Dispatchers.IO) {
			try {
				val repo = MangaProviderFactory.create(page.source)
				val url = repo.getPageFullUrl(page)
				val request = Request.Builder()
					.url(url)
					.get()
					.build()
				val uri = get<OkHttpClient>().newCall(request).await().use { response ->
					val fileName =
						URLUtil.guessFileName(url, response.contentDisposition, response.mimeType)
					MediaStoreCompat.insertImage(resolver, fileName) {
						response.body!!.byteStream().copyTo(it)
					}
				}
				withContext(Dispatchers.Main) {
					viewState.onPageSaved(uri)
				}
			} catch (e: CancellationException) {
			} catch (e: Exception) {
				withContext(Dispatchers.Main) {
					viewState.onPageSaved(null)
				}
			}
		}
	}

	companion object {

		fun saveState(state: ReaderState) {
			GlobalScope.launch(Dispatchers.IO) {
				HistoryRepository().addOrUpdate(
					manga = state.manga,
					chapterId = state.chapterId,
					page = state.page,
					scroll = state.scroll
				)
			}
		}

	}
}