package org.koitharu.kotatsu.core.ui.image

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.StyleRes
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withClip
import coil3.Image
import coil3.asImage
import coil3.getExtra
import coil3.request.ImageRequest
import com.google.android.material.color.MaterialColors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.util.KotatsuColors
import org.koitharu.kotatsu.core.util.ext.mangaSourceKey

open class FaviconDrawable(
	context: Context,
	@StyleRes styleResId: Int,
	name: String,
) : Drawable() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	protected var colorBackground = Color.WHITE
	protected var colorForeground = Color.DKGRAY
	private var colorStroke = Color.LTGRAY
	private val letter = name.take(1).uppercase()
	private var cornerSize = 0f
	private var intrinsicSize = -1
	private val textBounds = Rect()
	private val tempRect = Rect()
	private val boundsF = RectF()
	private val clipPath = Path()

	init {
		context.withStyledAttributes(styleResId, R.styleable.FaviconFallbackDrawable) {
			colorBackground = getColor(R.styleable.FaviconFallbackDrawable_backgroundColor, colorBackground)
			colorStroke = getColor(R.styleable.FaviconFallbackDrawable_strokeColor, colorStroke)
			cornerSize = getDimension(R.styleable.FaviconFallbackDrawable_cornerSize, cornerSize)
			paint.strokeWidth = getDimension(R.styleable.FaviconFallbackDrawable_strokeWidth, 0f) * 2f
			intrinsicSize = getDimensionPixelSize(R.styleable.FaviconFallbackDrawable_drawableSize, intrinsicSize)
		}
		paint.textAlign = Paint.Align.CENTER
		paint.isFakeBoldText = true
		colorForeground = MaterialColors.harmonize(KotatsuColors.random(name), colorBackground)
	}

	override fun draw(canvas: Canvas) {
		if (cornerSize > 0f) {
			canvas.withClip(clipPath) {
				doDraw(canvas)
			}
		} else {
			doDraw(canvas)
		}
	}

	override fun onBoundsChange(bounds: Rect) {
		super.onBoundsChange(bounds)
		boundsF.set(bounds)
		val innerWidth = bounds.width() - (paint.strokeWidth * 2f)
		paint.textSize = getTextSizeForWidth(innerWidth, letter) * 0.5f
		paint.getTextBounds(letter, 0, letter.length, textBounds)
		clipPath.reset()
		clipPath.addRoundRect(boundsF, cornerSize, cornerSize, Path.Direction.CW)
		clipPath.close()
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	override fun getIntrinsicWidth(): Int = intrinsicSize

	override fun getIntrinsicHeight(): Int = intrinsicSize

	@Suppress("DeprecatedCallableAddReplaceWith")
	@Deprecated("Deprecated in Java")
	override fun getOpacity() = PixelFormat.TRANSPARENT

	private fun doDraw(canvas: Canvas) {
		// background
		paint.color = colorBackground
		paint.style = Paint.Style.FILL
		canvas.drawPaint(paint)
		// letter
		paint.color = colorForeground
		val cx = (boundsF.left + boundsF.right) * 0.6f
		val ty = boundsF.bottom * 0.7f + textBounds.height() * 0.5f - textBounds.bottom
		canvas.drawText(letter, cx, ty, paint)
		if (paint.strokeWidth > 0f) {
			// stroke
			paint.color = colorStroke
			paint.style = Paint.Style.STROKE
			canvas.drawPath(clipPath, paint)
		}
	}

	private fun getTextSizeForWidth(width: Float, text: String): Float {
		val testTextSize = 48f
		paint.textSize = testTextSize
		paint.getTextBounds(text, 0, text.length, tempRect)
		return testTextSize * width / tempRect.width()
	}

	class Factory(
		@StyleRes private val styleResId: Int,
	) : ((ImageRequest) -> Image?) {

		override fun invoke(request: ImageRequest): Image? {
			val source = request.getExtra(mangaSourceKey) ?: return null
			val context = request.context
			val title = source.getTitle(context)
			return FaviconDrawable(context, styleResId, title).asImage()
		}
	}
}
