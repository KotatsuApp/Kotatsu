package org.koitharu.kotatsu.reader.ui.colorfilter

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import coil3.request.error
import coil3.size.Scale
import coil3.size.ViewSizeResolver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPage
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.decodeRegion
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.indicator
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.setChecked
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.databinding.ActivityColorFilterBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ColorFilterConfigActivity :
	BaseActivity<ActivityColorFilterBinding>(),
	Slider.OnChangeListener,
	View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel: ColorFilterConfigViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityColorFilterBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		viewBinding.sliderBrightness.addOnChangeListener(this)
		viewBinding.sliderContrast.addOnChangeListener(this)
		val formatter = PercentLabelFormatter(resources)
		viewBinding.sliderContrast.setLabelFormatter(formatter)
		viewBinding.sliderBrightness.setLabelFormatter(formatter)
		viewBinding.switchInvert.setOnCheckedChangeListener(this)
		viewBinding.switchGrayscale.setOnCheckedChangeListener(this)
		viewBinding.buttonDone.setOnClickListener(this)
		viewBinding.buttonReset.setOnClickListener(this)

		onBackPressedDispatcher.addCallback(ColorFilterConfigBackPressedDispatcher(this, viewModel))

		viewModel.colorFilter.observe(this, this::onColorFilterChanged)
		viewModel.isLoading.observe(this, this::onLoadingChanged)
		viewModel.onDismiss.observeEvent(this) {
			finishAfterTransition()
		}
		loadPreview(viewModel.preview)
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			when (slider.id) {
				R.id.slider_brightness -> viewModel.setBrightness(value)
				R.id.slider_contrast -> viewModel.setContrast(value)
			}
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		when (buttonView.id) {
			R.id.switch_invert -> viewModel.setInversion(isChecked)
			R.id.switch_grayscale -> viewModel.setGrayscale(isChecked)
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> showSaveConfirmation()
			R.id.button_reset -> viewModel.reset()
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.scrollView.updatePadding(
			bottom = insets.bottom,
		)
		viewBinding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top
		}
	}

	fun showSaveConfirmation() {
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.apply)
			.setMessage(R.string.color_correction_apply_text)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.this_manga) { _, _ ->
				viewModel.save()
			}.setNeutralButton(R.string.globally) { _, _ ->
				viewModel.saveGlobally()
			}.show()
	}

	private fun onColorFilterChanged(readerColorFilter: ReaderColorFilter?) {
		viewBinding.sliderBrightness.setValueRounded(readerColorFilter?.brightness ?: 0f)
		viewBinding.sliderContrast.setValueRounded(readerColorFilter?.contrast ?: 0f)
		viewBinding.switchInvert.setChecked(readerColorFilter?.isInverted ?: false, false)
		viewBinding.switchGrayscale.setChecked(readerColorFilter?.isGrayscale ?: false, false)
		viewBinding.imageViewAfter.colorFilter = readerColorFilter?.toColorFilter()
	}

	private fun loadPreview(page: MangaPage) {
		val data: Any = page.preview?.takeUnless { it.isEmpty() } ?: page
		ImageRequest.Builder(this@ColorFilterConfigActivity)
			.data(data)
			.scale(Scale.FILL)
			.decodeRegion()
			.mangaSourceExtra(page.source)
			.bitmapConfig(if (viewModel.is32BitColorsEnabled) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565)
			.indicator(listOf(viewBinding.progressBefore, viewBinding.progressAfter))
			.error(R.drawable.ic_error_placeholder)
			.size(ViewSizeResolver(viewBinding.imageViewBefore))
			.target(DoubleViewTarget(viewBinding.imageViewBefore, viewBinding.imageViewAfter))
			.enqueueWith(coil)
	}

	private fun onLoadingChanged(isLoading: Boolean) {
		viewBinding.sliderContrast.isEnabled = !isLoading
		viewBinding.sliderBrightness.isEnabled = !isLoading
		viewBinding.switchInvert.isEnabled = !isLoading
		viewBinding.switchGrayscale.isEnabled = !isLoading
		viewBinding.buttonDone.isEnabled = !isLoading
	}

	private class PercentLabelFormatter(resources: Resources) : LabelFormatter {

		private val pattern = resources.getString(R.string.percent_string_pattern)

		override fun getFormattedValue(value: Float): String {
			val percent = ((value + 1f) * 100).format(0)
			return pattern.format(percent)
		}
	}

	companion object {

		const val EXTRA_PAGES = "pages"
		const val EXTRA_MANGA = "manga_id"

		fun newIntent(context: Context, manga: Manga, page: MangaPage) =
			Intent(context, ColorFilterConfigActivity::class.java)
				.putExtra(EXTRA_MANGA, ParcelableManga(manga))
				.putExtra(EXTRA_PAGES, ParcelableMangaPage(page))
	}
}
