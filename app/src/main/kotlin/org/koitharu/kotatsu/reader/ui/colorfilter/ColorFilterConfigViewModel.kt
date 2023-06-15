package org.koitharu.kotatsu.reader.ui.colorfilter

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPage
import org.koitharu.kotatsu.core.parser.MangaDataRepository
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
	private val mangaDataRepository: MangaDataRepository,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<ParcelableManga>(EXTRA_MANGA).manga

	private var initialColorFilter: ReaderColorFilter? = null
	val colorFilter = MutableStateFlow<ReaderColorFilter?>(null)
	val onDismiss = MutableEventFlow<Unit>()
	val preview = savedStateHandle.require<ParcelableMangaPage>(ColorFilterConfigActivity.EXTRA_PAGES).page

	val isChanged: Boolean
		get() = colorFilter.value != initialColorFilter

	init {
		launchLoadingJob {
			initialColorFilter = mangaDataRepository.getColorFilter(manga.id)
			colorFilter.value = initialColorFilter
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
