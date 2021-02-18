package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import androidx.annotation.DrawableRes
import androidx.core.view.children
import com.google.android.material.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.koitharu.kotatsu.utils.ext.getThemeColor

class ChipsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = R.attr.chipGroupStyle
) : ChipGroup(context, attrs, defStyleAttr) {

	private var isLayoutSuppressedCompat = false
	private var isLayoutCalledOnSuppressed = false
	private var chipOnClickListener = OnClickListener {
		onChipClickListener?.onChipClick(it as Chip, it.tag)
	}
	var onChipClickListener: OnChipClickListener? = null
		set(value) {
			field = value
			val isChipClickable = value != null
			children.forEach { it.isClickable = isChipClickable }
		}

	override fun requestLayout() {
		if (isLayoutSuppressedCompat) {
			isLayoutCalledOnSuppressed = true
		} else {
			super.requestLayout()
		}
	}

	fun setChips(items: List<ChipModel>) {
		suppressLayoutCompat(true)
		try {
			for ((i, model) in items.withIndex()) {
				val chip = getChildAt(i) as Chip? ?: addChip()
				bindChip(chip, model)
			}
			for (i in items.size until childCount) {
				removeViewAt(i)
			}
		} finally {
			suppressLayoutCompat(false)
		}
	}

	private fun bindChip(chip: Chip, model: ChipModel) {
		chip.text = model.title
		if (model.icon == 0) {
			chip.isChipIconVisible = false
		} else {
			chip.isCheckedIconVisible = true
			chip.setChipIconResource(model.icon)
		}
		chip.tag = model.data
	}

	private fun addChip(): Chip {
		val chip = Chip(context)
		chip.setTextColor(context.getThemeColor(android.R.attr.textColorPrimary))
		chip.isCloseIconVisible = false
		chip.setEnsureMinTouchTargetSize(false)
		chip.setOnClickListener(chipOnClickListener)
		chip.isClickable = onChipClickListener != null
		addView(chip)
		return chip
	}

	private fun suppressLayoutCompat(suppress: Boolean) {
		isLayoutSuppressedCompat = suppress
		if (!suppress) {
			if (isLayoutCalledOnSuppressed) {
				requestLayout()
				isLayoutCalledOnSuppressed = false
			}
		}
	}

	data class ChipModel(
		@DrawableRes val icon: Int,
		val title: CharSequence,
		val data: Any? = null
	)

	fun interface OnChipClickListener {

		fun onChipClick(chip: Chip, data: Any?)
	}
}