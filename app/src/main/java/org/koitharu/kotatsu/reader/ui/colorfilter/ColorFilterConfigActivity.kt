package org.koitharu.kotatsu.reader.ui.colorfilter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.ViewSizeResolver
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPages
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.databinding.ActivityColorFilterBinding
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.referer
import javax.inject.Inject
import kotlin.math.roundToInt
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ColorFilterConfigActivity : BaseActivity<ActivityColorFilterBinding>(), Slider.OnChangeListener {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var mangaRepositoryFacotry: MangaRepository.Factory

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityColorFilterBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		binding.sliderLightness.addOnChangeListener(this)
		binding.sliderSaturation.addOnChangeListener(this)
		initPreview()
		updateFilter()
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		updateFilter()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.scrollView.updatePadding(
			bottom = insets.bottom,
		)
		binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top
		}
	}

	private fun updateFilter() {

		fun Int.toColor() = Color.rgb(this, this, this)

		val cf = LightingColorFilter(
			binding.sliderSaturation.value.roundToInt().toColor(),
			binding.sliderLightness.value.roundToInt().toColor(),
		)
		binding.imageViewAfter.colorFilter = cf
	}

	private fun initPreview() {
		val page = intent?.getParcelableExtra<ParcelableMangaPages>(EXTRA_PAGES)?.pages?.firstOrNull()
		if (page == null) {
			finishAfterTransition()
			return
		}
		lifecycleScope.launch {
			val repository = mangaRepositoryFacotry.create(page.source)
			val url = repository.getPageUrl(page)
			ImageRequest.Builder(this@ColorFilterConfigActivity)
				.data(url)
				.referer(page.referer)
				.scale(Scale.FILL)
				.size(ViewSizeResolver(binding.imageViewBefore))
				.allowRgb565(false)
				.target(ShadowViewTarget(binding.imageViewBefore, binding.imageViewAfter))
				.enqueueWith(coil)
		}
	}

	companion object {

		private const val EXTRA_PAGES = "pages"

		fun newIntent(context: Context, page: MangaPage) = Intent(context, ColorFilterConfigActivity::class.java)
			.putExtra(EXTRA_PAGES, ParcelableMangaPages(listOf(page)))
	}
}
