package org.koitharu.kotatsu.reader.ui.colorfilter

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPage
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter
import org.koitharu.kotatsu.reader.ui.colorfilter.ColorFilterConfigActivity.Companion.EXTRA_MANGA
import javax.inject.Inject

@HiltViewModel
class ColorFilterConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val settings: AppSettings,
	private val mangaDataRepository: MangaDataRepository,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<ParcelableManga>(EXTRA_MANGA).manga

	private var initialColorFilter: ReaderColorFilter? = null
	val colorFilter = MutableStateFlow<ReaderColorFilter?>(null)
	val onDismiss = MutableEventFlow<Unit>()
	val preview = savedStateHandle.require<ParcelableMangaPage>(ColorFilterConfigActivity.EXTRA_PAGES).page

	val isChanged: Boolean
		get() = colorFilter.value != initialColorFilter

	val is32BitColorsEnabled: Boolean
		get() = settings.is32BitColorsEnabled

	init {
		launchLoadingJob {
			initialColorFilter = mangaDataRepository.getColorFilter(manga.id) ?: settings.readerColorFilter
			colorFilter.value = initialColorFilter
		}
	}

	fun setBrightness(brightness: Float) {
		updateColorFilter { it.copy(brightness = brightness) }
	}

	fun setContrast(contrast: Float) {
		updateColorFilter { it.copy(contrast = contrast) }
	}

	fun setInversion(invert: Boolean) {
		updateColorFilter { it.copy(isInverted = invert) }
	}

	fun setGrayscale(grayscale: Boolean) {
		updateColorFilter { it.copy(isGrayscale = grayscale) }
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

	fun saveGlobally() {
		launchLoadingJob(Dispatchers.Default) {
			settings.readerColorFilter = colorFilter.value
			if (mangaDataRepository.getColorFilter(manga.id) != null) {
				mangaDataRepository.saveColorFilter(manga, colorFilter.value)
			}
			onDismiss.call(Unit)
		}
	}

	private inline fun updateColorFilter(block: (ReaderColorFilter) -> ReaderColorFilter) {
		colorFilter.value = block(
			colorFilter.value ?: ReaderColorFilter.EMPTY,
		).takeUnless { it.isEmpty }
	}
}
