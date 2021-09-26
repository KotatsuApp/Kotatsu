package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.chip.ChipGroup
import org.koitharu.kotatsu.R

class ChipsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = com.google.android.material.R.attr.chipGroupStyle
) : ChipGroup(context, attrs, defStyleAttr) {

	private var isLayoutSuppressedCompat = false
	private var isLayoutCalledOnSuppressed = false
	private var chipOnClickListener = OnClickListener {
		onChipClickListener?.onChipClick(it as Chip, it.tag)
	}
	private var chipOnCloseListener = OnClickListener {
		onChipCloseClickListener?.onChipCloseClick(it as Chip, it.tag)
	}
	var onChipClickListener: OnChipClickListener? = null
		set(value) {
			field = value
			val isChipClickable = value != null
			children.forEach { it.isClickable = isChipClickable }
		}
	var onChipCloseClickListener: OnChipCloseClickListener? = null
		set(value) {
			field = value
			val isCloseIconVisible = value != null
			children.forEach { (it as? Chip)?.isCloseIconVisible = isCloseIconVisible }
		}

	override fun requestLayout() {
		if (isLayoutSuppressedCompat) {
			isLayoutCalledOnSuppressed = true
		} else {
			super.requestLayout()
		}
	}

	fun setChips(items: Collection<ChipModel>) {
		suppressLayoutCompat(true)
		try {
			for ((i, model) in items.withIndex()) {
				val chip = getChildAt(i) as Chip? ?: addChip()
				bindChip(chip, model)
			}
			if (childCount > items.size) {
				removeViews(items.size, childCount - items.size)
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
		chip.isClickable = onChipClickListener != null
		chip.tag = model.data
	}

	private fun addChip(): Chip {
		val chip = Chip(context)
		val drawable = ChipDrawable.createFromAttributes(context, null, 0, R.style.Widget_Kotatsu_Chip)
		chip.setChipDrawable(drawable)
		chip.setTextColor(ContextCompat.getColor(context, R.color.color_primary))
		chip.isCloseIconVisible = onChipCloseClickListener != null
		chip.setOnCloseIconClickListener(chipOnCloseListener)
		chip.setEnsureMinTouchTargetSize(false)
		chip.setOnClickListener(chipOnClickListener)
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

	fun interface OnChipCloseClickListener {

		fun onChipCloseClick(chip: Chip, data: Any?)
	}
}