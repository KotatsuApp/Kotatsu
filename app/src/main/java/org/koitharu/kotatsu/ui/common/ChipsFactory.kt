package org.koitharu.kotatsu.ui.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import com.google.android.material.chip.Chip
import com.google.android.material.shape.CornerFamily
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.getThemeColor

class ChipsFactory(private val context: Context) {

	fun create(convertView: Chip? = null, text: CharSequence, @DrawableRes iconRes: Int = 0, tag: Any? = null): Chip {
		val chip = convertView ?: Chip(context).apply {
			setTextColor(context.getThemeColor(android.R.attr.textColorPrimary))
			isCloseIconVisible = false
		}
		chip.text = text
		if (iconRes == 0) {
			chip.isChipIconVisible = false
		} else {
			chip.isCheckedIconVisible = true
			chip.setChipIconResource(iconRes)
		}
		chip.tag = tag
		return chip
	}
}