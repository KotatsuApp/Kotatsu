package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import org.koitharu.kotatsu.R

class IconsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

	private var iconSize = LayoutParams.WRAP_CONTENT
	private var iconSpacing = 0

	val iconsCount: Int
		get() {
			var count = 0
			repeat(childCount) { i ->
				if (getChildAt(i).isVisible) {
					count++
				}
			}
			return count
		}

	init {
		context.withStyledAttributes(attrs, R.styleable.IconsView) {
			iconSize = getDimensionPixelSize(R.styleable.IconsView_iconSize, iconSize)
			iconSpacing = getDimensionPixelOffset(R.styleable.IconsView_iconSpacing, iconSpacing)
		}
	}

	fun setIcons(icons: Iterable<Drawable>) {
		var index = 0
		for (icon in icons) {
			val imageView = (getChildAt(index) as ImageView?) ?: addImageView()
			imageView.setImageDrawable(icon)
			imageView.isVisible = true
			index++
		}
		for (i in index until childCount) {
			val imageView = getChildAt(i) as? ImageView ?: continue
			imageView.setImageDrawable(null)
			imageView.isVisible = false
		}
	}

	fun clearIcons() {
		repeat(childCount) { i ->
			getChildAt(i).isVisible = false
		}
	}

	fun addIcon(drawable: Drawable) {
		val imageView = getNextImageView()
		imageView.setImageDrawable(drawable)
		imageView.isVisible = true
	}

	fun addIcon(@DrawableRes resId: Int) {
		val imageView = getNextImageView()
		imageView.setImageResource(resId)
		imageView.isVisible = true
	}

	private fun getNextImageView(): ImageView {
		repeat(childCount) { i ->
			val child = getChildAt(i)
			if (child is ImageView && !child.isVisible) {
				return child
			}
		}
		return addImageView()
	}

	private fun addImageView() = ImageView(context).also {
		it.scaleType = ImageView.ScaleType.FIT_CENTER
		val lp = LayoutParams(iconSize, iconSize)
		if (isNotEmpty()) {
			lp.marginStart = iconSpacing
		}
		addView(it, lp)
	}
}
