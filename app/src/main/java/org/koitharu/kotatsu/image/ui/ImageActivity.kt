package org.koitharu.kotatsu.image.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.target.ViewTarget
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityImageBinding
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.indicator

@AndroidEntryPoint
class ImageActivity : BaseActivity<ActivityImageBinding>() {

	@Inject
	lateinit var coil: ImageLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityImageBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(false)
		}
		loadImage(intent.data)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		with(binding.toolbar) {
			updatePadding(
				left = insets.left,
				right = insets.right,
			)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
	}

	private fun loadImage(url: Uri?) {
		ImageRequest.Builder(this)
			.data(url)
			.memoryCachePolicy(CachePolicy.DISABLED)
			.lifecycle(this)
			.target(SsivTarget(binding.ssiv))
			.indicator(binding.progressBar)
			.enqueueWith(coil)
	}

	private class SsivTarget(
		override val view: SubsamplingScaleImageView,
	) : ViewTarget<SubsamplingScaleImageView> {

		override fun onError(error: Drawable?) = setDrawable(error)

		override fun onSuccess(result: Drawable) = setDrawable(result)

		override fun equals(other: Any?): Boolean {
			return (this === other) || (other is SsivTarget && view == other.view)
		}

		override fun hashCode() = view.hashCode()

		override fun toString() = "SsivTarget(view=$view)"

		private fun setDrawable(drawable: Drawable?) {
			if (drawable != null) {
				view.setImage(ImageSource.Bitmap(drawable.toBitmap()))
			} else {
				view.recycle()
			}
		}
	}

	companion object {

		fun newIntent(context: Context, url: String): Intent {
			return Intent(context, ImageActivity::class.java)
				.setData(Uri.parse(url))
		}
	}
}
