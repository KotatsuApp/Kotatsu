package org.koitharu.kotatsu.image.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.Image
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.lifecycle
import coil3.target.ViewTarget
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.util.PopupMenuMediator
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getDisplayIcon
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivityImageBinding
import org.koitharu.kotatsu.databinding.ItemErrorStateBinding
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ImageActivity : BaseActivity<ActivityImageBinding>(), ImageRequest.Listener, View.OnClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private var errorBinding: ItemErrorStateBinding? = null
	private val viewModel: ImageViewModel by viewModels()
	private lateinit var menuMediator: PopupMenuMediator

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityImageBinding.inflate(layoutInflater))
		viewBinding.buttonBack.setOnClickListener(this)
		viewBinding.buttonMenu.setOnClickListener(this)
		val imageUrl = requireNotNull(intent.data)

		val menuProvider = ImageMenuProvider(
			activity = this,
			snackbarHost = viewBinding.root,
			viewModel = viewModel,
		)
		menuMediator = PopupMenuMediator(menuProvider)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.root, null))
		viewModel.onImageSaved.observeEvent(this, ::onImageSaved)
		loadImage(imageUrl)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.buttonBack.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top + bottomMargin
			leftMargin = insets.left + bottomMargin
			rightMargin = insets.right + bottomMargin
		}
		viewBinding.buttonMenu.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top + bottomMargin
			leftMargin = insets.left + bottomMargin
			rightMargin = insets.right + bottomMargin
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_back -> dispatchNavigateUp()
			R.id.button_menu -> menuMediator.onLongClick(v)
			else -> loadImage(intent.data)
		}
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
			.mangaSourceExtra(MangaSource(intent.getStringExtra(EXTRA_SOURCE)))
			.target(SsivTarget(viewBinding.ssiv))
			.enqueueWith(coil)
	}

	private fun onImageSaved(uri: Uri) {
		Snackbar.make(viewBinding.root, R.string.page_saved, Snackbar.LENGTH_LONG)
			.setAction(R.string.share) {
				ShareHelper(this).shareImage(uri)
			}.show()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val button = viewBinding.buttonMenu
		button.isClickable = !isLoading
		if (isLoading) {
			button.setImageDrawable(
				CircularProgressDrawable(this).also {
					it.setStyle(CircularProgressDrawable.LARGE)
					it.setColorSchemeColors(getThemeColor(com.google.android.material.R.attr.colorControlNormal))
					it.start()
				},
			)
		} else {
			button.setImageResource(materialR.drawable.abc_ic_menu_overflow_material)
		}
	}

	private class SsivTarget(
		override val view: SubsamplingScaleImageView,
	) : ViewTarget<SubsamplingScaleImageView> {

		override fun onError(error: Image?) = setDrawable(error?.asDrawable(view.resources))

		override fun onSuccess(result: Image) = setDrawable(result.asDrawable(view.resources))

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

		const val EXTRA_SOURCE = "source"

		fun newIntent(context: Context, url: String, source: MangaSource?): Intent {
			return Intent(context, ImageActivity::class.java)
				.setData(Uri.parse(url))
				.putExtra(EXTRA_SOURCE, source?.name)
		}
	}
}
