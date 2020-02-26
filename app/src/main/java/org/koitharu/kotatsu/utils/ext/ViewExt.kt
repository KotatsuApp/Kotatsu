package org.koitharu.kotatsu.utils.ext

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.koitharu.kotatsu.ui.common.ChipsFactory

fun View.hideKeyboard() {
	val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.showKeyboard() {
	val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.showSoftInput(this, 0)
}

val EditText.plainText
	get() = text?.toString().orEmpty()

inline fun <reified T : View> ViewGroup.inflate(@LayoutRes resId: Int) =
	LayoutInflater.from(context).inflate(resId, this, false) as T

val TextView.hasText get() = !text.isNullOrEmpty()

fun RecyclerView.lookupSpanSize(callback: (Int) -> Int) {
	(layoutManager as? GridLayoutManager)?.spanSizeLookup =
		object : GridLayoutManager.SpanSizeLookup() {
			override fun getSpanSize(position: Int) = callback(position)
		}
}

val RecyclerView.hasItems: Boolean
	get() = (adapter?.itemCount ?: 0) > 0

var TextView.drawableStart: Drawable?
	get() = compoundDrawablesRelative[0]
	set(value) {
		val old = compoundDrawablesRelative
		setCompoundDrawablesRelativeWithIntrinsicBounds(value, old[1], old[2], old[3])
	}

var TextView.drawableEnd: Drawable?
	get() = compoundDrawablesRelative[2]
	set(value) {
		val old = compoundDrawablesRelative
		setCompoundDrawablesRelativeWithIntrinsicBounds(old[0], old[1], value, old[3])
	}

var TextView.textAndVisible: CharSequence?
	get() = text?.takeIf { visibility == View.VISIBLE }
	set(value) {
		text = value
		isGone = value.isNullOrEmpty()
	}

fun <T> ChipGroup.setChips(data: Iterable<T>, action: ChipsFactory.(T) -> Chip) {
	removeAllViews()
	val factory = ChipsFactory(context)
	data.forEach {
		val chip = factory.action(it)
		addView(chip)
	}
}

fun RecyclerView.clearItemDecorations() {
	while (itemDecorationCount > 0) {
		removeItemDecorationAt(0)
	}
}

var RecyclerView.firstItem: Int
	get() = (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
		?: RecyclerView.NO_POSITION
	set(value) {
		if (value != RecyclerView.NO_POSITION) {
			(layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(value, 0)
		}
	}

fun View.disableFor(timeInMillis: Long) {
	isEnabled = false
	postDelayed(timeInMillis) {
		isEnabled = true
	}
}

fun View.showPopupMenu(
	@MenuRes menuRes: Int, onPrepare: ((Menu) -> Unit)? = null,
	onItemClick: (MenuItem) -> Boolean
) {
	val menu = PopupMenu(context, this)
	menu.inflate(menuRes)
	menu.setOnMenuItemClickListener(onItemClick)
	onPrepare?.invoke(menu.menu)
	menu.show()
}

fun ViewGroup.hitTest(x: Int, y: Int): Set<View> {
	val result = HashSet<View>(4)
	val rect = Rect()
	for (child in children) {
		if (child.isVisible && child.getGlobalVisibleRect(rect)) {
			if (rect.contains(x, y)) {
				if (child is ViewGroup) {
					result += child.hitTest(x, y)
				} else {
					result += child
				}
			}
		}
	}
	return result
}

fun View.hasGlobalPoint(x: Int, y: Int): Boolean {
	if (visibility != View.VISIBLE) {
		return false
	}
	val rect = Rect()
	getGlobalVisibleRect(rect)
	return rect.contains(x, y)
}

fun DrawerLayout.toggleDrawer(gravity: Int) {
	if (isDrawerOpen(gravity)) {
		closeDrawer(gravity)
	} else {
		openDrawer(gravity)
	}
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

fun ViewPager2.doOnPageChanged(callback: (Int) -> Unit) {
	registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

		override fun onPageSelected(position: Int) {
			super.onPageSelected(position)
			callback(position)
		}
	})
}