package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.chip.ChipGroup
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.castOrNull

import com.google.android.material.R as materialR

class ChipsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = com.google.android.material.R.attr.chipGroupStyle,
) : ChipGroup(context, attrs, defStyleAttr) {

	private var isLayoutSuppressedCompat = false
	private var isLayoutCalledOnSuppressed = false
	private val chipOnClickListener = OnClickListener {
		onChipClickListener?.onChipClick(it as Chip, it.tag)
	}
	private val chipOnCloseListener = OnClickListener {
		val chip = it as Chip
		val data = it.tag
		onChipCloseClickListener?.onChipCloseClick(chip, data) ?: onChipClickListener?.onChipClick(chip, data)
	}
	private val chipStyle: Int
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

	init {
		val ta = context.obtainStyledAttributes(attrs, R.styleable.ChipsView, defStyleAttr, 0)
		chipStyle = ta.getResourceId(R.styleable.ChipsView_chipStyle, R.style.Widget_Kotatsu_Chip)
		ta.recycle()

		if (isInEditMode) {
			setChips(
				List(5) {
					ChipModel(title = "Chip $it")
				},
			)
		}
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

	fun <T> getCheckedData(cls: Class<T>): Set<T> {
		val result = LinkedHashSet<T>(childCount)
		for (child in children) {
			if (child is Chip && child.isChecked) {
				result += cls.castOrNull(child.tag) ?: continue
			}
		}
		return result
	}

	private fun bindChip(chip: Chip, model: ChipModel) {
		if (model.titleResId == 0) {
			chip.text = model.title
		} else {
			chip.setText(model.titleResId)
		}
		chip.isClickable = onChipClickListener != null || model.isCheckable
		chip.isCheckable = model.isCheckable
		if (model.icon == 0) {
			chip.chipIcon = null
			chip.isChipIconVisible = false
		} else {
			chip.setChipIconResource(model.icon)
			chip.isChipIconVisible = true
		}
		chip.isChecked = model.isChecked
		chip.isCheckedIconVisible = chip.isCheckable && model.icon == 0
		chip.isCloseIconVisible = if (onChipCloseClickListener != null || model.isDropdown) {
			chip.setCloseIconResource(
				if (model.isDropdown) R.drawable.ic_expand_more else materialR.drawable.ic_m3_chip_close,
			)
			true
		} else {
			false
		}
		chip.tag = model.data
	}

	private fun addChip(): Chip {
		val chip = Chip(context)
		val drawable = ChipDrawable.createFromAttributes(context, null, 0, chipStyle)
		chip.setChipDrawable(drawable)
		chip.isChipIconVisible = false
		chip.setOnCloseIconClickListener(chipOnCloseListener)
		chip.setEnsureMinTouchTargetSize(false)
		chip.setOnClickListener(chipOnClickListener)
		chip.isElegantTextHeight = false
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
		val title: CharSequence? = null,
		@StringRes val titleResId: Int = 0,
		@DrawableRes val icon: Int = 0,
		val isCheckable: Boolean = false,
		@ColorRes val tint: Int = 0,
		val isChecked: Boolean = false,
		val isDropdown: Boolean = false,
		val data: Any? = null,
	)

	fun interface OnChipClickListener {

		fun onChipClick(chip: Chip, data: Any?)
	}

	fun interface OnChipCloseClickListener {

		fun onChipCloseClick(chip: Chip, data: Any?)
	}
}
