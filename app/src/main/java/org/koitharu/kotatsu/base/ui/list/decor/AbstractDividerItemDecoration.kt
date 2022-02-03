package org.koitharu.kotatsu.base.ui.list.decor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.res.getColorOrThrow
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as materialR

@SuppressLint("PrivateResource")
abstract class AbstractDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

	private val bounds = Rect()
	private val thickness: Int
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	init {
		paint.style = Paint.Style.FILL
		val ta = context.obtainStyledAttributes(
			null,
			materialR.styleable.MaterialDivider,
			materialR.attr.materialDividerStyle,
			materialR.style.Widget_Material3_MaterialDivider,
		)
		paint.color = ta.getColorOrThrow(materialR.styleable.MaterialDivider_dividerColor)
		thickness = ta.getDimensionPixelSize(
			materialR.styleable.MaterialDivider_dividerThickness,
			context.resources.getDimensionPixelSize(materialR.dimen.material_divider_thickness),
		)
		ta.recycle()
	}


	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State,
	) {
		outRect.set(0, thickness, 0, 0)
	}

	// TODO implement for horizontal lists on demand
	override fun onDraw(canvas: Canvas, parent: RecyclerView, s: RecyclerView.State) {
		if (parent.layoutManager == null || thickness == 0) {
			return
		}
		canvas.save()
		val left: Float
		val right: Float
		if (parent.clipToPadding) {
			left = parent.paddingLeft.toFloat()
			right = (parent.width - parent.paddingRight).toFloat()
			canvas.clipRect(
				left,
				parent.paddingTop.toFloat(),
				right,
				(parent.height - parent.paddingBottom).toFloat()
			)
		} else {
			left = 0f
			right = parent.width.toFloat()
		}

		var previous: RecyclerView.ViewHolder? = null
		for (child in parent.children) {
			val holder = parent.getChildViewHolder(child)
			if (previous != null && shouldDrawDivider(previous, holder)) {
				parent.getDecoratedBoundsWithMargins(child, bounds)
				val top: Float = bounds.top + child.translationY
				val bottom: Float = top + thickness
				canvas.drawRect(left, top, right, bottom, paint)
			}
			previous = holder
		}
		canvas.restore()
	}

	protected abstract fun shouldDrawDivider(
		above: RecyclerView.ViewHolder,
		below: RecyclerView.ViewHolder,
	): Boolean
}