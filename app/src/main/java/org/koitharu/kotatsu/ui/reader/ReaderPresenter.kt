package org.koitharu.kotatsu.ui.reader

import android.content.ContentResolver
import android.util.Log
import android.webkit.URLUtil
import kotlinx.coroutines.*
import moxy.InjectViewState
import moxy.presenterScope
import okhttp3.OkHttpClient
import okhttp3.Request
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

	private var loaderJob: Job? = null
	private var isInitialized = false

	fun loadChapter(manga: Manga, chapterId: Long, action: ReaderAction) {
		loaderJob?.cancel()
		loaderJob = presenterScope.launch {
			viewState.onLoadingStateChanged(isLoading = true)
			try {
				withContext(Dispatchers.IO) {
					val repo = MangaProviderFactory.create(manga.source)
					val chapter = (manga.chapters ?: repo.getDetails(manga).chapters?.also {
						withContext(Dispatchers.Main) {
							viewState.onChaptersLoader(it)
						}
					})?.find { it.id == chapterId }
						?: throw RuntimeException("Chapter ${chapterId} not found")
					val pages = repo.getPages(chapter)
					if (!isInitialized) {
						val prefs = MangaDataRepository()
						var mode = prefs.getReaderMode(manga.id)
						if (mode == null) {
							mode = MangaUtils.determineReaderMode(pages)
							if (mode != null) {
								prefs.savePreferences(
									mangaId = manga.id,
									mode = mode
								)
							}
						}
						withContext(Dispatchers.Main) {
							viewState.onInitReader(mode ?: ReaderMode.UNKNOWN)
						}
						isInitialized = true
					}
					withContext(Dispatchers.Main) {
						viewState.onPagesLoaded(chapterId, pages, action)
					}
				}
			} catch (e: CancellationException){
				Log.w(null, "Loader job cancelled", e)
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(isLoading = false)
			}
		}
	}

	fun saveState(state: ReaderState, mode: ReaderMode? = null) {
		presenterScope.launch(Dispatchers.IO) {
			HistoryRepository().addOrUpdate(
				manga = state.manga,
				chapterId = state.chapterId,
				page = state.page
			)
			if (mode != null) {
				MangaDataRepository().savePreferences(
					mangaId = state.manga.id,
					mode = mode
				)
			}
		}
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
				val uri = getKoin().get<OkHttpClient>().newCall(request).await().use { response ->
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

	override fun onDestroy() {
		instance = null
		super.onDestroy()
	}

	companion object {

		private var instance: ReaderPresenter? = null

		fun getInstance(): ReaderPresenter = instance ?: synchronized(this) {
			ReaderPresenter().also {
				instance = it
			}
		}
	}

}