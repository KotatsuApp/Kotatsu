package org.koitharu.kotatsu.reader.ui

import android.content.ContentResolver
import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaUtils
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.utils.MediaStoreCompat
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.*

class ReaderViewModel(
	private val dataRepository: MangaDataRepository,
	private val settings: AppSettings
) : BaseViewModel() {

	val reader = MutableLiveData<Pair<Manga, ReaderMode>>()
	val onPageSaved = SingleLiveEvent<Uri?>()

	fun init(manga: Manga) {
		launchLoadingJob {
			val mode = withContext(Dispatchers.Default) {
				val repo = manga.source.repository
				val chapter =
					(manga.chapters ?: throw RuntimeException("Chapters is null")).random()
				var mode = dataRepository.getReaderMode(manga.id)
				if (mode == null) {
					val pages = repo.getPages(chapter)
					val isWebtoon = MangaUtils.determineMangaIsWebtoon(pages)
					mode = getReaderMode(isWebtoon)
					if (isWebtoon != null) {
						dataRepository.savePreferences(
							manga = manga,
							mode = mode
						)
					}
				}
				mode
			}
			reader.value = manga to mode
		}
	}

	fun setMode(manga: Manga, mode: ReaderMode) {
		launchJob {
			dataRepository.savePreferences(
				manga = manga,
				mode = mode
			)
			reader.value = manga to mode
		}
	}

	fun savePage(resolver: ContentResolver, page: MangaPage) {
		launchJob {
			withContext(Dispatchers.Default) {
				try {
					val repo = page.source.repository
					val url = repo.getPageFullUrl(page)
					val request = Request.Builder()
						.url(url)
						.get()
						.build()
					val uri = get<OkHttpClient>().newCall(request).await().use { response ->
						val fileName =
							URLUtil.guessFileName(
								url,
								response.contentDisposition,
								response.mimeType
							)
						MediaStoreCompat.insertImage(resolver, fileName) {
							response.body!!.byteStream().copyTo(it)
						}
					}
					onPageSaved.postCall(uri)
				} catch (e: CancellationException) {
				} catch (e: Exception) {
					onPageSaved.postCall(null)
				}
			}
		}
	}

	private fun getReaderMode(isWebtoon: Boolean?) = when {
		isWebtoon == true -> ReaderMode.WEBTOON
		settings.isPreferRtlReader -> ReaderMode.REVERSED
		else -> ReaderMode.STANDARD
	}

	companion object : KoinComponent {

		fun saveState(state: ReaderState) {
			processLifecycleScope.launch(Dispatchers.Default + IgnoreErrors) {
				get<HistoryRepository>().addOrUpdate(
					manga = state.manga,
					chapterId = state.chapterId,
					page = state.page,
					scroll = state.scroll
				)
			}
		}

	}
}