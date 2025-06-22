package org.koitharu.kotatsu.core.image

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.asImage
import coil3.request.Disposable
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.NullRequestData
import coil3.request.SuccessResult
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.request.lifecycle
import coil3.request.target
import coil3.size.Scale
import coil3.size.Size
import coil3.size.SizeResolver
import coil3.size.ViewSizeResolver
import coil3.util.CoilUtils
import com.google.android.material.imageview.ShapeableImageView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.decodeRegion
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.isNetworkError
import java.util.LinkedList
import javax.inject.Inject

@AndroidEntryPoint
open class CoilImageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : ShapeableImageView(context, attrs, defStyleAttr), ImageRequest.Listener {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var networkState: NetworkState

	var allowRgb565: Boolean = false
	var useExistingDrawable: Boolean = false
	var decodeRegion: Boolean = false
	var exactImageSize: Size? = null
	var crossfadeDurationFactor: Float = 1f

	var placeholderDrawable: Drawable? = null
	var errorDrawable: Drawable? = null
	var fallbackDrawable: Drawable? = null

	private var currentRequest: Disposable? = null
	private var currentImageData: Any = NullRequestData
	private var networkWaitingJob: Job? = null

	private var listeners: MutableList<ImageRequest.Listener>? = null

	val isFailed: Boolean
		get() = CoilUtils.result(this) is ErrorResult

	init {
		context.withStyledAttributes(attrs, R.styleable.CoilImageView, defStyleAttr) {
			allowRgb565 = getBoolean(R.styleable.CoilImageView_allowRgb565, allowRgb565)
			useExistingDrawable = getBoolean(R.styleable.CoilImageView_useExistingDrawable, useExistingDrawable)
			decodeRegion = getBoolean(R.styleable.CoilImageView_decodeRegion, decodeRegion)
			placeholderDrawable = getDrawable(R.styleable.CoilImageView_placeholderDrawable)
			errorDrawable = getDrawable(R.styleable.CoilImageView_errorDrawable)
			fallbackDrawable = getDrawable(R.styleable.CoilImageView_fallbackDrawable)
			crossfadeDurationFactor = if (getBoolean(R.styleable.CoilImageView_crossfadeEnabled, true)) {
				crossfadeDurationFactor
			} else {
				0f
			}
		}
	}

	override fun onCancel(request: ImageRequest) {
		super.onCancel(request)
		listeners?.forEach { it.onCancel(request) }
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		super.onError(request, result)
		listeners?.forEach { it.onError(request, result) }
		if (result.throwable.isNetworkError()) {
			waitForNetwork()
		}
	}

	override fun onStart(request: ImageRequest) {
		super.onStart(request)
		listeners?.forEach { it.onStart(request) }
	}

	override fun onSuccess(request: ImageRequest, result: SuccessResult) {
		super.onSuccess(request, result)
		listeners?.forEach { it.onSuccess(request, result) }
	}

	fun addImageRequestListener(listener: ImageRequest.Listener) {
		val list = listeners ?: LinkedList<ImageRequest.Listener>().also { listeners = it }
		list.add(listener)
	}

	fun removeImageRequestListener(listener: ImageRequest.Listener) {
		listeners?.remove(listener)
	}

	fun setImageAsync(@DrawableRes resourceId: Int) = enqueueRequest(
		newRequestBuilder()
			.data(resourceId)
			.build(),
	)

	fun setImageAsync(url: String?) = enqueueRequest(
		newRequestBuilder()
			.data(url)
			.build(),
	)

	fun disposeImage() {
		networkWaitingJob?.cancel()
		networkWaitingJob = null
		CoilUtils.dispose(this)
		currentRequest = null
		currentImageData = NullRequestData
		setImageDrawable(null)
	}

	fun reload() {
		CoilUtils.result(this)?.let { result ->
			enqueueRequest(result.request, force = true)
		}
	}

	protected fun enqueueRequest(request: ImageRequest, force: Boolean = false): Disposable {
		val previous = currentRequest
		if (!force && currentImageData == request.data && previous?.job?.isCancelled == false && !isFailed) {
			return previous
		}
		networkWaitingJob?.cancel()
		networkWaitingJob = null
		currentImageData = request.data
		return coil.enqueue(request).also { currentRequest = it }
	}

	protected open fun newRequestBuilder() = ImageRequest.Builder(context).apply {
		lifecycle(findViewTreeLifecycleOwner())
		val crossfadeDuration = if (context.isAnimationsEnabled) {
			(context.getAnimationDuration(R.integer.config_defaultAnimTime) * crossfadeDurationFactor).toInt()
		} else {
			0
		}
		crossfade(crossfadeDuration)
		if (useExistingDrawable) {
			val previousDrawable = this@CoilImageView.drawable?.asImage()
			if (previousDrawable != null) {
				fallback(previousDrawable)
				placeholder(previousDrawable)
				error(previousDrawable)
			} else {
				setupPlaceholders()
			}
		} else {
			setupPlaceholders()
		}
		if (decodeRegion) {
			decodeRegion(0)
		}
		size(
			exactImageSize?.let {
				SizeResolver(it)
			} ?: ViewSizeResolver(this@CoilImageView),
		)
		scale(scaleType.toCoilScale())
		listener(this@CoilImageView)
		allowRgb565(allowRgb565)
		target(this@CoilImageView)
	}

	private fun ImageRequest.Builder.setupPlaceholders() {
		placeholder(placeholderDrawable?.asImage())
		error(errorDrawable?.asImage())
		fallback(fallbackDrawable?.asImage())
	}

	private fun ScaleType.toCoilScale(): Scale = if (this == ScaleType.CENTER_CROP) {
		Scale.FILL
	} else {
		Scale.FIT
	}

	private fun waitForNetwork() {
		if (networkWaitingJob?.isActive == true || networkState.isOnline()) {
			return
		}
		networkWaitingJob?.cancel()
		networkWaitingJob = findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
			networkState.awaitForConnection()
			if (isFailed) {
				reload()
			}
		}
	}
}
