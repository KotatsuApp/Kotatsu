package org.koitharu.kotatsu.utils.ext

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isGone
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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