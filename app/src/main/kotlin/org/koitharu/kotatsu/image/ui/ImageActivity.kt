package org.koitharu.kotatsu.image.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.target.ViewTarget
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getDisplayIcon
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.databinding.ActivityImageBinding
import org.koitharu.kotatsu.databinding.ItemErrorStateBinding
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject

@AndroidEntryPoint
class ImageActivity : BaseActivity<ActivityImageBinding>(), ImageRequest.Listener, View.OnClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private var errorBinding: ItemErrorStateBinding? = null

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
		with(viewBinding.toolbar) {
			updatePadding(
				left = insets.left,
				right = insets.right,
			)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
	}

	override fun onClick(v: View?) {
		loadImage(intent.data)
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		viewBinding.progressBar.hide()
		with(errorBinding ?: ItemErrorStateBinding.bind(viewBinding.stubError.inflate())) {
			errorBinding = this
			root.isVisible = true
			textViewError.text = result.throwable.getDisplayMessage(resources)
			textViewError.setCompoundDrawablesWithIntrinsicBounds(0, result.throwable.getDisplayIcon(), 0, 0)
			buttonRetry.isVisible = true
			buttonRetry.setOnClickListener(this@ImageActivity)
		}
	}

	override fun onStart(request: ImageRequest) {
		viewBinding.progressBar.show()
		(errorBinding?.root ?: viewBinding.stubError).isVisible = false
	}

	override fun onSuccess(request: ImageRequest, result: SuccessResult) {
		viewBinding.progressBar.hide()
		(errorBinding?.root ?: viewBinding.stubError).isVisible = false
	}

	private fun loadImage(url: Uri?) {
		ImageRequest.Builder(this)
			.data(url)
			.memoryCachePolicy(CachePolicy.DISABLED)
			.lifecycle(this)
			.listener(this)
			.tag(intent.getSerializableExtraCompat<MangaSource>(EXTRA_SOURCE))
			.target(SsivTarget(viewBinding.ssiv))
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

		private const val EXTRA_SOURCE = "source"

		fun newIntent(context: Context, url: String, source: MangaSource?): Intent {
			return Intent(context, ImageActivity::class.java)
				.setData(Uri.parse(url))
				.putExtra(EXTRA_SOURCE, source)
		}
	}
}
