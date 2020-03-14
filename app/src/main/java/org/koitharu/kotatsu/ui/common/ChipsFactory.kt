package org.koitharu.kotatsu.ui.common

import android.content.Context
import android.view.View
import androidx.annotation.DrawableRes
import com.google.android.material.chip.Chip
import org.koitharu.kotatsu.utils.ext.getThemeColor

class ChipsFactory(private val context: Context) {

	fun create(convertView: Chip? = null, text: CharSequence, @DrawableRes iconRes: Int = 0,
			   tag: Any? = null, onClickListener: View.OnClickListener? = null): Chip {
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
		chip.setOnClickListener(onClickListener)
		return chip
	}
}