package org.koitharu.kotatsu.utils.ext

import android.app.Activity
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.koitharu.kotatsu.core.ui.ChipsFactory

fun View.hideKeyboard() {
	val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.showKeyboard() {
	val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.showSoftInput(this, 0)
}

inline fun <reified T : View> ViewGroup.inflate(@LayoutRes resId: Int) =
	LayoutInflater.from(context).inflate(resId, this, false) as T

val RecyclerView.hasItems: Boolean
	get() = (adapter?.itemCount ?: 0) > 0

var TextView.textAndVisible: CharSequence?
	get() = text?.takeIf { visibility == View.VISIBLE }
	set(value) {
		text = value
		isGone = value.isNullOrEmpty()
	}

fun <T> ChipGroup.addChips(data: Iterable<T>, action: ChipsFactory.(T) -> Chip) {
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

inline fun View.showPopupMenu(
	@MenuRes menuRes: Int,
	onPrepare: (Menu) -> Unit = {},
	onItemClick: PopupMenu.OnMenuItemClickListener
) {
	val menu = PopupMenu(context, this)
	menu.inflate(menuRes)
	menu.setOnMenuItemClickListener(onItemClick)
	onPrepare(menu.menu)
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

inline fun ViewPager2.doOnPageChanged(crossinline callback: (Int) -> Unit) {
	registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

		override fun onPageSelected(position: Int) {
			super.onPageSelected(position)
			callback(position)
		}
	})
}

val ViewPager2.recyclerView: RecyclerView?
	get() = children.find { it is RecyclerView } as? RecyclerView

fun View.resetTransformations() {
	alpha = 1f
	translationX = 0f
	translationY = 0f
	translationZ = 0f
	scaleX = 1f
	scaleY = 1f
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

fun RecyclerView.callOnScrollListeners() {
	try {
		val field = RecyclerView::class.java.getDeclaredField("mScrollListeners")
		field.isAccessible = true
		val listeners = field.get(this) as List<*>
		for (x in listeners) {
			(x as RecyclerView.OnScrollListener).onScrolled(this, 0, 0)
		}
	} catch (e: Throwable) {
		Log.e(null, "RecyclerView.callOnScrollListeners() failed", e)
	}
}

fun ViewPager2.callOnPageChaneListeners() {
	try {
		val field = ViewPager2::class.java.getDeclaredField("mExternalPageChangeCallbacks")
		field.isAccessible = true
		val compositeCallback = field.get(this)
		val field2 = compositeCallback.javaClass.getDeclaredField("mCallbacks")
		field2.isAccessible = true
		val listeners = field2.get(compositeCallback) as List<*>
		val position = currentItem
		for (x in listeners) {
			(x as ViewPager2.OnPageChangeCallback).onPageSelected(position)
		}
	} catch (e: Throwable) {
		Log.e(null, "ViewPager2.callOnPageChaneListeners() failed", e)
	}
}

fun RecyclerView.findCenterViewPosition(): Int {
	val centerX = width / 2f
	val centerY = height / 2f
	val view = findChildViewUnder(centerX, centerY) ?: return RecyclerView.NO_POSITION
	return getChildAdapterPosition(view)
}

fun ViewPager2.swapAdapter(newAdapter: RecyclerView.Adapter<*>?) {
	val position = currentItem
	adapter = newAdapter
	if (adapter != null && position != RecyclerView.NO_POSITION) {
		setCurrentItem(position, false)
	}
}