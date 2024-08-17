package org.koitharu.kotatsu.core.util.ext

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.Checkable
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.core.view.descendants
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import kotlin.math.roundToInt

fun View.hasGlobalPoint(x: Int, y: Int): Boolean {
	if (visibility != View.VISIBLE) {
		return false
	}
	val rect = Rect()
	getGlobalVisibleRect(rect)
	return rect.contains(x, y)
}

fun View.measureHeight(): Int {
	val vh = height
	return if (vh == 0) {
		measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
		measuredHeight
	} else vh
}

fun View.measureWidth(): Int {
	val vw = width
	return if (vw == 0) {
		measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
		measuredWidth
	} else vw
}

inline fun ViewPager2.doOnPageChanged(crossinline callback: (Int) -> Unit) {
	registerOnPageChangeCallback(
		object : ViewPager2.OnPageChangeCallback() {

			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)
				callback(position)
			}
		},
	)
}

val ViewPager2.recyclerView: RecyclerView?
	get() = children.firstNotNullOfOrNull { it as? RecyclerView }

fun ViewPager2.findCurrentViewHolder(): ViewHolder? {
	return recyclerView?.findViewHolderForAdapterPosition(currentItem)
}

fun View.resetTransformations() {
	alpha = 1f
	translationX = 0f
	translationY = 0f
	translationZ = 0f
	scaleX = 1f
	scaleY = 1f
	rotation = 0f
	rotationX = 0f
	rotationY = 0f
}

fun Slider.setValueRounded(newValue: Float) {
	val step = stepSize
	val roundedValue = if (step <= 0f) {
		newValue
	} else {
		(newValue / step).roundToInt() * step
	}
	value = roundedValue.coerceIn(valueFrom, valueTo)
}

fun RecyclerView.invalidateNestedItemDecorations() {
	descendants.filterIsInstance<RecyclerView>().forEach {
		it.invalidateItemDecorations()
	}
}

val View.parentView: ViewGroup?
	get() = parent as? ViewGroup

@Suppress("UnusedReceiverParameter")
fun View.measureDimension(desiredSize: Int, measureSpec: Int): Int {
	var result: Int
	val specMode = MeasureSpec.getMode(measureSpec)
	val specSize = MeasureSpec.getSize(measureSpec)
	if (specMode == MeasureSpec.EXACTLY) {
		result = specSize
	} else {
		result = desiredSize
		if (specMode == MeasureSpec.AT_MOST) {
			result = result.coerceAtMost(specSize)
		}
	}
	return result
}

fun <V> V.setChecked(checked: Boolean, animate: Boolean) where V : View, V : Checkable {
	val skipAnimation = !animate && checked != isChecked
	isChecked = checked
	if (skipAnimation) {
		jumpDrawablesToCurrentState()
	}
}

var View.isRtl: Boolean
	get() = layoutDirection == View.LAYOUT_DIRECTION_RTL
	set(value) {
		layoutDirection = if (value) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
	}

fun TabLayout.setTabsEnabled(enabled: Boolean) {
	for (i in 0 until tabCount) {
		getTabAt(i)?.view?.isEnabled = enabled
	}
}

fun BaseProgressIndicator<*>.showOrHide(value: Boolean) {
	if (value) {
		if (!isVisible) show()
	} else {
		if (isVisible) hide()
	}
}

fun View.setOnContextClickListenerCompat(listener: View.OnLongClickListener) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		setOnContextClickListener(listener::onLongClick)
	}
}

val Toolbar.menuView: ActionMenuView?
	get() {
		menu // to call ensureMenu()
		return children.firstNotNullOfOrNull { it as? ActionMenuView }
	}

fun MaterialButton.setProgressIcon() {
	val progressDrawable = CircularProgressDrawable(context)
	progressDrawable.strokeWidth = resources.resolveDp(2f)
	progressDrawable.setColorSchemeColors(currentTextColor)
	progressDrawable.setTintList(textColors)
	icon = progressDrawable
	progressDrawable.start()
}

fun Chip.setProgressIcon() {
	val progressDrawable = CircularProgressDrawable(context)
	progressDrawable.strokeWidth = resources.resolveDp(2f)
	progressDrawable.setColorSchemeColors(currentTextColor)
	chipIcon = progressDrawable
	progressDrawable.start()
}
