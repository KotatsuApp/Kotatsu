package org.koitharu.kotatsu.utils.ext

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.core.view.isGone

var TextView.textAndVisible: CharSequence?
	inline get() = text?.takeIf { visibility == View.VISIBLE }
	inline set(value) {
		text = value
		isGone = value.isNullOrEmpty()
	}

var TextView.drawableStart: Drawable?
	inline get() = compoundDrawablesRelative[0]
	set(value) {
		val dr = compoundDrawablesRelative
		setCompoundDrawablesRelativeWithIntrinsicBounds(value, dr[1], dr[2], dr[3])
	}