package org.koitharu.kotatsu.reader.ui.colorfilter

import androidx.lifecycle.MutableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter
import org.koitharu.kotatsu.utils.SingleLiveEvent

class ColorFilterConfigViewModel @AssistedInject constructor(
	@Assisted private val manga: Manga,
	@Assisted page: MangaPage,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val mangaDataRepository: MangaDataRepository,
) : BaseViewModel() {

	private var initialColorFilter: ReaderColorFilter? = null
	val colorFilter = MutableLiveData<ReaderColorFilter?>(null)
	val onDismiss = SingleLiveEvent<Unit>()
	val preview = MutableLiveData<MangaPage?>(null)

	val isChanged: Boolean
		get() = colorFilter.value != initialColorFilter

	init {
		launchLoadingJob {
			initialColorFilter = mangaDataRepository.getColorFilter(manga.id)
			colorFilter.value = initialColorFilter
		}
		launchLoadingJob {
			val repository = mangaRepositoryFactory.create(page.source)
			val url = repository.getPageUrl(page)
			preview.value = MangaPage(
				id = page.id,
				url = url,
				referer = page.referer,
				preview = page.preview,
				source = page.source,
			)
		}
	}

	fun setBrightness(brightness: Float) {
		val cf = colorFilter.value
		colorFilter.value = ReaderColorFilter(brightness, cf?.contrast ?: 0f).takeUnless { it.isEmpty }
	}

	fun setContrast(contrast: Float) {
		val cf = colorFilter.value
		colorFilter.value = ReaderColorFilter(cf?.brightness ?: 0f, contrast).takeUnless { it.isEmpty }
	}

	fun reset() {
		colorFilter.value = null
	}

	fun save() {
		launchLoadingJob {
			mangaDataRepository.saveColorFilter(manga, colorFilter.value)
			onDismiss.call(Unit)
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(manga: Manga, page: MangaPage): ColorFilterConfigViewModel
	}
}
