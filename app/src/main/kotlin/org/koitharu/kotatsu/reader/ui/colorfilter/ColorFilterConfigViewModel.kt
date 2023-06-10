package org.koitharu.kotatsu.reader.ui.colorfilter

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPages
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter
import org.koitharu.kotatsu.reader.ui.colorfilter.ColorFilterConfigActivity.Companion.EXTRA_MANGA
import javax.inject.Inject

@HiltViewModel
class ColorFilterConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val mangaDataRepository: MangaDataRepository,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<ParcelableManga>(EXTRA_MANGA).manga

	private var initialColorFilter: ReaderColorFilter? = null
	val colorFilter = MutableStateFlow<ReaderColorFilter?>(null)
	val onDismiss = MutableEventFlow<Unit>()
	val preview = MutableStateFlow<MangaPage?>(null)

	val isChanged: Boolean
		get() = colorFilter.value != initialColorFilter

	init {
		val page = savedStateHandle.require<ParcelableMangaPages>(ColorFilterConfigActivity.EXTRA_PAGES).pages.first()
		launchLoadingJob {
			initialColorFilter = mangaDataRepository.getColorFilter(manga.id)
			colorFilter.value = initialColorFilter
		}
		launchLoadingJob(Dispatchers.Default) {
			val repository = mangaRepositoryFactory.create(page.source)
			val url = repository.getPageUrl(page)
			preview.value = MangaPage(
				id = page.id,
				url = url,
				preview = page.preview,
				source = page.source,
			)
		}
	}

	fun setBrightness(brightness: Float) {
		val cf = colorFilter.value
		colorFilter.value = ReaderColorFilter(
			brightness = brightness,
			contrast = cf?.contrast ?: 0f,
			isInverted = cf?.isInverted ?: false,
		).takeUnless { it.isEmpty }
	}

	fun setContrast(contrast: Float) {
		val cf = colorFilter.value
		colorFilter.value = ReaderColorFilter(
			brightness = cf?.brightness ?: 0f,
			contrast = contrast,
			isInverted = cf?.isInverted ?: false,
		).takeUnless { it.isEmpty }
	}

	fun setInversion(invert: Boolean) {
		val cf = colorFilter.value
		if (invert == cf?.isInverted) {
			return
		}
		colorFilter.value = ReaderColorFilter(
			brightness = cf?.brightness ?: 0f,
			contrast = cf?.contrast ?: 0f,
			isInverted = invert,
		).takeUnless { it.isEmpty }
	}

	fun reset() {
		colorFilter.value = null
	}

	fun save() {
		launchLoadingJob(Dispatchers.Default) {
			mangaDataRepository.saveColorFilter(manga, colorFilter.value)
			onDismiss.call(Unit)
		}
	}
}
