package org.koitharu.kotatsu.core.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withClip
import com.google.android.material.drawable.DrawableUtils
import org.koitharu.kotatsu.R

class ShapeView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private val corners = FloatArray(8)
	private val outlinePath = Path()
	private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

	init {
		context.withStyledAttributes(attrs, R.styleable.ShapeView, defStyleAttr) {
			val cornerSize = getDimension(R.styleable.ShapeView_cornerSize, 0f)
			corners[0] = getDimension(R.styleable.ShapeView_cornerSizeTopLeft, cornerSize)
			corners[1] = corners[0]
			corners[2] = getDimension(R.styleable.ShapeView_cornerSizeTopRight, cornerSize)
			corners[3] = corners[2]
			corners[4] = getDimension(R.styleable.ShapeView_cornerSizeBottomRight, cornerSize)
			corners[5] = corners[4]
			corners[6] = getDimension(R.styleable.ShapeView_cornerSizeBottomLeft, cornerSize)
			corners[7] = corners[6]
			strokePaint.color = getColor(R.styleable.ShapeView_strokeColor, Color.TRANSPARENT)
			strokePaint.strokeWidth = getDimension(R.styleable.ShapeView_strokeWidth, 0f)
			strokePaint.style = Paint.Style.STROKE
		}
		outlineProvider = OutlineProvider()
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		if (w != oldw || h != oldh) {
			rebuildPath()
		}
	}

	override fun draw(canvas: Canvas) {
		canvas.withClip(outlinePath) {
			super.draw(canvas)
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		if (strokePaint.strokeWidth > 0f) {
			canvas.drawPath(outlinePath, strokePaint)
		}
	}

	private fun rebuildPath() {
		outlinePath.reset()
		val w = width
		val h = height
		if (w > 0 && h > 0) {
			outlinePath.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), corners, Path.Direction.CW)
		}
	}

	private inner class OutlineProvider : ViewOutlineProvider() {

		@SuppressLint("RestrictedApi")
		override fun getOutline(view: View?, outline: Outline) {
			val corner = corners[0]
			var isRoundRect = true
			for (item in corners) {
				if (item != corner) {
					isRoundRect = false
					break
				}
			}
			if (isRoundRect) {
				outline.setRoundRect(0, 0, width, height, corner)
			} else {
				DrawableUtils.setOutlineToPath(outline, outlinePath)
			}
		}
	}
}
