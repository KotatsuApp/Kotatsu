package org.koitharu.kotatsu.reader.ui.colorfilter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPages
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter
import org.koitharu.kotatsu.reader.ui.colorfilter.ColorFilterConfigActivity.Companion.EXTRA_MANGA
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.emitValue
import javax.inject.Inject

@HiltViewModel
class ColorFilterConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val mangaDataRepository: MangaDataRepository,
) : BaseViewModel() {

	private val manga = checkNotNull(savedStateHandle.get<ParcelableManga>(EXTRA_MANGA)?.manga)

	private var initialColorFilter: ReaderColorFilter? = null
	val colorFilter = MutableLiveData<ReaderColorFilter?>(null)
	val onDismiss = SingleLiveEvent<Unit>()
	val preview = MutableLiveData<MangaPage?>(null)

	val isChanged: Boolean
		get() = colorFilter.value != initialColorFilter

	init {
		val page = checkNotNull(
			savedStateHandle.get<ParcelableMangaPages>(ColorFilterConfigActivity.EXTRA_PAGES)?.pages?.firstOrNull(),
		)
		launchLoadingJob {
			initialColorFilter = mangaDataRepository.getColorFilter(manga.id)
			colorFilter.value = initialColorFilter
		}
		launchLoadingJob(Dispatchers.Default) {
			val repository = mangaRepositoryFactory.create(page.source)
			val url = repository.getPageUrl(page)
			preview.emitValue(
				MangaPage(
					id = page.id,
					url = url,
					preview = page.preview,
					source = page.source,
				),
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
		launchLoadingJob(Dispatchers.Default) {
			mangaDataRepository.saveColorFilter(manga, colorFilter.value)
			onDismiss.emitCall(Unit)
		}
	}
}
