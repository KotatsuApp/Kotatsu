package org.koitharu.kotatsu.utils.ext

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.Slider
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewHolder
import kotlin.math.roundToInt

fun View.hideKeyboard() {
	val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.showKeyboard() {
	val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.showSoftInput(this, 0)
}

fun RecyclerView.clearItemDecorations() {
	suppressLayout(true)
	while (itemDecorationCount > 0) {
		removeItemDecorationAt(0)
	}
	suppressLayout(false)
}

var RecyclerView.firstVisibleItemPosition: Int
	get() = (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
		?: RecyclerView.NO_POSITION
	set(value) {
		if (value != RecyclerView.NO_POSITION) {
			(layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(value, 0)
		}
	}

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
		measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
		measuredHeight
	} else vh
}

fun View.measureWidth(): Int {
	val vw = width
	return if (vw == 0) {
		measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
		measuredWidth
	} else vw
}

inline fun ViewPager2.doOnPageChanged(crossinline callback: (Int) -> Unit) {
	registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

		override fun onPageSelected(position: Int) {
			super.onPageSelected(position)
			callback(position)
		}
	})
}

val ViewPager2.recyclerView: RecyclerView?
	get() = children.firstNotNullOfOrNull { it as? RecyclerView }

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

inline fun RecyclerView.doOnCurrentItemChanged(crossinline callback: (Int) -> Unit) {
	addOnScrollListener(object : RecyclerView.OnScrollListener() {

		private var lastItem = -1

		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			super.onScrolled(recyclerView, dx, dy)
			val item = recyclerView.findCenterViewPosition()
			if (item != RecyclerView.NO_POSITION && item != lastItem) {
				lastItem = item
				callback(item)
			}
		}
	})
}

fun RecyclerView.findCenterViewPosition(): Int {
	val centerX = width / 2f
	val centerY = height / 2f
	val view = findChildViewUnder(centerX, centerY) ?: return RecyclerView.NO_POSITION
	return getChildAdapterPosition(view)
}

fun <T> RecyclerView.ViewHolder.getItem(clazz: Class<T>): T? {
	val rawItem = when (this) {
		is AdapterDelegateViewBindingViewHolder<*, *> -> item
		is AdapterDelegateViewHolder<*> -> item
		else -> null
	} ?: return null
	return if (clazz.isAssignableFrom(rawItem.javaClass)) {
		clazz.cast(rawItem)
	} else {
		null
	}
}

fun Slider.setValueRounded(newValue: Float) {
	val step = stepSize
	value = (newValue / step).roundToInt() * step
}

val RecyclerView.isScrolledToTop: Boolean
	get() {
		if (childCount == 0) {
			return true
		}
		val holder = findViewHolderForAdapterPosition(0)
		return holder != null && holder.itemView.top >= 0
	}

fun <T : View> ViewGroup.findViewsByType(clazz: Class<T>): Sequence<T> {
	if (childCount == 0) {
		return emptySequence()
	}
	return sequence {
		for (view in children) {
			if (clazz.isInstance(view)) {
				yield(clazz.cast(view)!!)
			} else if (view is ViewGroup && view.childCount != 0) {
				yieldAll(view.findViewsByType(clazz))
			}
		}
	}
}