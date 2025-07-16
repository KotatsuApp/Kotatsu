package org.koitharu.kotatsu.reader.ui.config

import android.graphics.Bitmap
import android.view.View
import androidx.annotation.CheckResult
import androidx.collection.scatterSetOf
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderBackground
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.util.MediatorStateFlow
import org.koitharu.kotatsu.core.util.ext.isLowRamDevice
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter

data class ReaderSettings(
	val zoomMode: ZoomMode,
	val background: ReaderBackground,
	val colorFilter: ReaderColorFilter?,
	val isReaderOptimizationEnabled: Boolean,
	val bitmapConfig: Bitmap.Config,
	val isPagesNumbersEnabled: Boolean,
	val isPagesCropEnabledStandard: Boolean,
	val isPagesCropEnabledWebtoon: Boolean,
) {

	private constructor(settings: AppSettings, colorFilterOverride: ReaderColorFilter?) : this(
		zoomMode = settings.zoomMode,
		background = settings.readerBackground,
		colorFilter = colorFilterOverride?.takeUnless { it.isEmpty } ?: settings.readerColorFilter,
		isReaderOptimizationEnabled = settings.isReaderOptimizationEnabled,
		bitmapConfig = if (settings.is32BitColorsEnabled) {
			Bitmap.Config.ARGB_8888
		} else {
			Bitmap.Config.RGB_565
		},
		isPagesNumbersEnabled = settings.isPagesNumbersEnabled,
		isPagesCropEnabledStandard = settings.isPagesCropEnabled(ReaderMode.STANDARD),
		isPagesCropEnabledWebtoon = settings.isPagesCropEnabled(ReaderMode.WEBTOON),
	)

	fun applyBackground(view: View) {
		view.background = background.resolve(view.context)
		view.backgroundTintList = if (background.isLight(view.context)) {
			colorFilter?.getBackgroundTint()
		} else {
			null
		}
	}

	fun isPagesCropEnabled(isWebtoon: Boolean) = if (isWebtoon) {
		isPagesCropEnabledWebtoon
	} else {
		isPagesCropEnabledStandard
	}

	@CheckResult
	fun applyBitmapConfig(ssiv: SubsamplingScaleImageView): Boolean {
		val config = bitmapConfig
		return if (ssiv.regionDecoderFactory.bitmapConfig != config) {
			ssiv.regionDecoderFactory = if (ssiv.context.isLowRamDevice()) {
				SkiaImageRegionDecoder.Factory(config)
			} else {
				SkiaPooledImageRegionDecoder.Factory(config)
			}
			ssiv.bitmapDecoderFactory = SkiaImageDecoder.Factory(config)
			true
		} else {
			false
		}
	}

	class Producer @AssistedInject constructor(
		@Assisted private val mangaId: Flow<Long>,
		private val settings: AppSettings,
		private val mangaDataRepository: MangaDataRepository,
	) : MediatorStateFlow<ReaderSettings>(ReaderSettings(settings, null)) {

		private val settingsKeys = scatterSetOf(
			AppSettings.KEY_ZOOM_MODE,
			AppSettings.KEY_PAGES_NUMBERS,
			AppSettings.KEY_READER_BACKGROUND,
			AppSettings.KEY_32BIT_COLOR,
			AppSettings.KEY_READER_OPTIMIZE,
			AppSettings.KEY_CF_CONTRAST,
			AppSettings.KEY_CF_BRIGHTNESS,
			AppSettings.KEY_CF_INVERTED,
			AppSettings.KEY_CF_GRAYSCALE,
			AppSettings.KEY_READER_CROP,
		)
		private var job: Job? = null

		override fun onActive() {
			assert(job?.isActive != true)
			job?.cancel()
			job = processLifecycleScope.launch(Dispatchers.Default) {
				observeImpl()
			}
		}

		override fun onInactive() {
			job?.cancel()
			job = null
		}

		private suspend fun observeImpl() {
			combine(
				mangaId.flatMapLatest { mangaDataRepository.observeColorFilter(it) },
				settings.observeChanges().filter { x -> x == null || x in settingsKeys }.onStart { emit(null) },
			) { mangaCf, settingsKey ->
				ReaderSettings(settings, mangaCf)
			}.collect {
				publishValue(it)
			}
		}

		@AssistedFactory
		interface Factory {

			fun create(mangaId: Flow<Long>): Producer
		}
	}
}
