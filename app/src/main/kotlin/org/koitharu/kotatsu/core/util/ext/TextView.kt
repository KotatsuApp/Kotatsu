package org.koitharu.kotatsu.core.util.ext

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import androidx.core.view.isGone
import androidx.core.widget.TextViewCompat

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

var TextView.drawableTop: Drawable?
	inline get() = compoundDrawablesRelative[1]
	set(value) {
		val dr = compoundDrawablesRelative
		setCompoundDrawablesRelativeWithIntrinsicBounds(dr[0], value, dr[2], dr[3])
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

var TextView.isBold: Boolean
	get() = typeface.isBold
	set(value) {
		var style = typeface.style
		style = if (value) {
			style or Typeface.BOLD
		} else {
			style and Typeface.BOLD.inv()
		}
		setTypeface(typeface, style)
	}

fun TextView.setThemeTextAppearance(@AttrRes resId: Int, @StyleRes fallback: Int) {
	context.obtainStyledAttributes(intArrayOf(resId)).use {
		TextViewCompat.setTextAppearance(this, it.getResourceId(0, fallback))
	}
}

val TextView.isTextTruncated: Boolean
	get() {
		val l = layout ?: return false
		if (maxLines in 0 until l.lineCount) {
			return true
		}
		val layoutLines = l.lineCount
		return layoutLines > 0 && l.getEllipsisCount(layoutLines - 1) > 0
	}
