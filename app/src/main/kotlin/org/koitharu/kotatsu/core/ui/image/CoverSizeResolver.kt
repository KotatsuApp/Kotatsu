package org.koitharu.kotatsu.core.ui.image

import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.ImageView
import coil3.size.Dimension
import coil3.size.Size
import coil3.size.ViewSizeResolver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

private const val ASPECT_RATIO_HEIGHT = 18f
private const val ASPECT_RATIO_WIDTH = 13f

class CoverSizeResolver(
	override val view: ImageView,
) : ViewSizeResolver<ImageView> {

	override suspend fun size(): Size {
		// Fast path: the view is already measured.
		getSize()?.let { return it }

		// Slow path: wait for the view to be measured.
		return suspendCancellableCoroutine { continuation ->
			val viewTreeObserver = view.viewTreeObserver

			val preDrawListener = object : OnPreDrawListener {
				private var isResumed = false

				override fun onPreDraw(): Boolean {
					val size = getSize()
					if (size != null) {
						viewTreeObserver.removePreDrawListenerSafe(this)

						if (!isResumed) {
							isResumed = true
							continuation.resume(size)
						}
					}
					return true
				}
			}

			viewTreeObserver.addOnPreDrawListener(preDrawListener)

			continuation.invokeOnCancellation {
				viewTreeObserver.removePreDrawListenerSafe(preDrawListener)
			}
		}
	}

	private fun getSize(): Size? {
		var width = getWidth()
		var height = getHeight()
		when {
			width == null && height == null -> {
				return null
			}
			height == null && width != null -> {
				height = Dimension((width.px * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).roundToInt())
			}
			width == null && height != null -> {
				width = Dimension((height.px * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).roundToInt())
			}
		}
		return Size(checkNotNull(width), checkNotNull(height))
	}

	private fun getWidth() = getDimension(
		paramSize = view.layoutParams?.width ?: -1,
		viewSize = view.width,
		paddingSize = if (subtractPadding) view.paddingLeft + view.paddingRight else 0
	)

	private fun getHeight() = getDimension(
		paramSize = view.layoutParams?.height ?: -1,
		viewSize = view.height,
		paddingSize = if (subtractPadding) view.paddingTop + view.paddingBottom else 0
	)

	private fun getDimension(paramSize: Int, viewSize: Int, paddingSize: Int): Dimension.Pixels? {
		if (paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
			return null
		}
		val insetParamSize = paramSize - paddingSize
		if (insetParamSize > 0) {
			return Dimension(insetParamSize)
		}
		val insetViewSize = viewSize - paddingSize
		if (insetViewSize > 0) {
			return Dimension(insetViewSize)
		}
		return null
	}

	private fun ViewTreeObserver.removePreDrawListenerSafe(victim: OnPreDrawListener) {
		if (isAlive) {
			removeOnPreDrawListener(victim)
		} else {
			view.viewTreeObserver.removeOnPreDrawListener(victim)
		}
	}
}
