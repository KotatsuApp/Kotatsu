package org.koitharu.kotatsu.ui.main.list.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.main.list.MangaListView
import org.koitharu.kotatsu.utils.MediaStoreCompat
import org.koitharu.kotatsu.utils.ext.safe
import org.koitharu.kotatsu.utils.ext.sub
import java.io.File
import java.io.IOException

@InjectViewState
class LocalListPresenter : BasePresenter<MangaListView<File>>() {

	private lateinit var repository: LocalMangaRepository

	override fun onFirstViewAttach() {
		repository = MangaProviderFactory.create(MangaSource.LOCAL) as LocalMangaRepository
		super.onFirstViewAttach()
	}

	fun loadList() {
		presenterScope.launch {
			viewState.onLoadingChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					repository.getList(0)
				}
				viewState.onListChanged(list)
			} catch (e: CancellationException) {
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingChanged(false)
			}
		}
	}

	fun importFile(context: Context, uri: Uri) {
		presenterScope.launch(Dispatchers.IO) {
			try {
				val name = MediaStoreCompat.getName(context, uri)
					?: throw IOException("Cannot fetch name from uri: $uri")
				if (!LocalMangaRepository.isFileSupported(name)) {
					throw UnsupportedFileException("Unsupported file on $uri")
				}
				val dest = context.getExternalFilesDir("manga")?.sub(name)
					?: throw IOException("External files dir unavailable")
				context.contentResolver.openInputStream(uri)?.use { source ->
					dest.outputStream().use { output ->
						source.copyTo(output)
					}
				} ?: throw IOException("Cannot open input stream: $uri")
				val list = repository.getList(0)
				withContext(Dispatchers.Main) {
					viewState.onListChanged(list)
				}
			} catch (e: CancellationException) {
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				withContext(Dispatchers.Main) {
					viewState.onError(e)
				}
			}
		}
	}

	fun delete(manga: Manga) {
		presenterScope.launch {
			try {
				withContext(Dispatchers.IO) {
					repository.delete(manga) || throw IOException("Unable to delete file")
					safe {
						HistoryRepository().delete(manga)
					}
				}
				viewState.onItemRemoved(manga)
			} catch (e: CancellationException) {
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
			}
		}
	}
}