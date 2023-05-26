package org.koitharu.kotatsu.core.ui.image

import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.ImageView
import coil.size.Dimension
import coil.size.Size
import coil.size.SizeResolver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

private const val ASPECT_RATIO_HEIGHT = 18f
private const val ASPECT_RATIO_WIDTH = 13f

class CoverSizeResolver(
	private val imageView: ImageView,
) : SizeResolver {

	override suspend fun size(): Size {
		getSize()?.let { return it }
		return suspendCancellableCoroutine { cont ->
			val layoutListener = LayoutListener(cont)
			imageView.addOnLayoutChangeListener(layoutListener)
			cont.invokeOnCancellation {
				imageView.removeOnLayoutChangeListener(layoutListener)
			}
		}
	}

	private fun getSize(): Size? {
		val lp = imageView.layoutParams
		var width = getDimension(lp.width, imageView.width, imageView.paddingLeft + imageView.paddingRight)
		var height = getDimension(lp.height, imageView.height, imageView.paddingTop + imageView.paddingBottom)
		if (width == null && height == null) {
			return null
		}
		if (height == null && width != null) {
			height = Dimension((width.px * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).roundToInt())
		} else if (width == null && height != null) {
			width = Dimension((height.px * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).roundToInt())
		}
		return Size(checkNotNull(width), checkNotNull(height))
	}

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

	private inner class LayoutListener(
		private val continuation: CancellableContinuation<Size>,
	) : OnLayoutChangeListener {

		override fun onLayoutChange(
			v: View,
			left: Int,
			top: Int,
			right: Int,
			bottom: Int,
			oldLeft: Int,
			oldTop: Int,
			oldRight: Int,
			oldBottom: Int,
		) {
			val size = getSize() ?: return
			v.removeOnLayoutChangeListener(this)
			continuation.resume(size)
		}
	}
}
