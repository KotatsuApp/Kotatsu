package org.koitharu.kotatsu.utils.ext

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.core.view.isGone

var TextView.textAndVisible: CharSequence?
	get() = text?.takeIf { visibility == View.VISIBLE }
	set(value) {
		text = value
		isGone = value.isNullOrEmpty()
	}

var TextView.drawableStart: Drawable?
	inline get() = compoundDrawablesRelative[0]
	set(value) {
		val dr = compoundDrawablesRelative
		setCompoundDrawablesRelativeWithIntrinsicBounds(value, dr[1], dr[2], dr[3])
	}

var TextView.drawableEnd: Drawable?
	inline get() = compoundDrawablesRelative[2]
	set(value) {
		val dr = compoundDrawablesRelative
		setCompoundDrawablesRelativeWithIntrinsicBounds(dr[0], dr[1], value, dr[3])
	}

fun TextView.setTextAndVisible(@StringRes textResId: Int) {
	if (textResId == 0) {
		text = null
		isGone = true
	} else {
		setText(textResId)
		isGone = text.isNullOrEmpty()
	}
}

fun TextView.setTextColorAttr(@AttrRes attrResId: Int) {
	setTextColor(context.getThemeColorStateList(attrResId))
}