package org.koitharu.kotatsu.core.ui.image

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.annotation.ReturnThis
import org.koitharu.kotatsu.core.util.ext.getThemeColorStateList
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.util.toIntUp
import com.google.android.material.R as materialR

class CardDrawable(
	context: Context,
	private var corners: Int,
) : Drawable() {

	private val cornerSize = context.resources.resolveDp(12f)
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val cornersF = FloatArray(8)
	private val boundsF = RectF()
	private val color: ColorStateList
	private val path = Path()
	private var alpha = 255
	private var state: IntArray? = null
	private var horizontalInset: Int = 0

	init {
		paint.style = Paint.Style.FILL
		color = context.getThemeColorStateList(materialR.attr.colorSurfaceContainerHighest)
			?: ColorStateList.valueOf(Color.TRANSPARENT)
		setCorners(corners)
		updateColor()
	}

	override fun draw(canvas: Canvas) {
		canvas.drawPath(path, paint)
	}

	override fun setAlpha(alpha: Int) {
		this.alpha = alpha
		updateColor()
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	override fun getColorFilter(): ColorFilter? = paint.colorFilter

	override fun getOutline(outline: Outline) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			outline.setPath(path)
		} else if (path.isConvex) {
			outline.setConvexPath(path)
		}
		outline.alpha = 1f
	}

	override fun getPadding(padding: Rect): Boolean {
		padding.set(
			horizontalInset,
			0,
			horizontalInset,
			0,
		)
		if (corners or TOP != 0) {
			padding.top += cornerSize.toIntUp()
		}
		if (corners or BOTTOM != 0) {
			padding.bottom += cornerSize.toIntUp()
		}
		return horizontalInset != 0
	}

	override fun onStateChange(state: IntArray): Boolean {
		this.state = state
		if (color.isStateful) {
			updateColor()
			return true
		} else {
			return false
		}
	}

	@Deprecated("Deprecated in Java")
	override fun getOpacity(): Int = PixelFormat.TRANSPARENT

	override fun onBoundsChange(bounds: Rect) {
		super.onBoundsChange(bounds)
		boundsF.set(bounds)
		boundsF.inset(horizontalInset.toFloat(), 0f)
		path.reset()
		path.addRoundRect(boundsF, cornersF, Path.Direction.CW)
		path.close()
	}

	@ReturnThis
	fun setCorners(corners: Int): CardDrawable {
		this.corners = corners
		val topLeft = if (corners and TOP_LEFT == TOP_LEFT) cornerSize else 0f
		val topRight = if (corners and TOP_RIGHT == TOP_RIGHT) cornerSize else 0f
		val bottomRight = if (corners and BOTTOM_RIGHT == BOTTOM_RIGHT) cornerSize else 0f
		val bottomLeft = if (corners and BOTTOM_LEFT == BOTTOM_LEFT) cornerSize else 0f
		cornersF[0] = topLeft
		cornersF[1] = topLeft
		cornersF[2] = topRight
		cornersF[3] = topRight
		cornersF[4] = bottomRight
		cornersF[5] = bottomRight
		cornersF[6] = bottomLeft
		cornersF[7] = bottomLeft
		invalidateSelf()
		return this
	}

	fun setHorizontalInset(inset: Int) {
		horizontalInset = inset
		invalidateSelf()
	}

	private fun updateColor() {
		paint.color = color.getColorForState(state, color.defaultColor)
		paint.alpha = alpha
	}

	companion object {

		const val TOP_LEFT = 1
		const val TOP_RIGHT = 2
		const val BOTTOM_LEFT = 4
		const val BOTTOM_RIGHT = 8

		const val LEFT = TOP_LEFT or BOTTOM_LEFT
		const val TOP = TOP_LEFT or TOP_RIGHT
		const val RIGHT = TOP_RIGHT or BOTTOM_RIGHT
		const val BOTTOM = BOTTOM_LEFT or BOTTOM_RIGHT

		const val NONE = 0
		const val ALL = TOP_LEFT or TOP_RIGHT or BOTTOM_RIGHT or BOTTOM_LEFT

		fun from(d: Drawable?): CardDrawable? = when (d) {
			null -> null
			is CardDrawable -> d
			is LayerDrawable -> (0 until d.numberOfLayers).firstNotNullOfOrNull { i ->
				from(d.getDrawable(i))
			}

			else -> null
		}
	}
}
