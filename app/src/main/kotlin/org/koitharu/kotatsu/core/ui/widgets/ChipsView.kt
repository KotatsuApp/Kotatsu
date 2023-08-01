package org.koitharu.kotatsu.core.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View.OnClickListener
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.getColorStateListOrThrow
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
		onChipCloseClickListener?.onChipCloseClick(it as Chip, it.tag)
	}
	private val defaultChipStrokeColor: ColorStateList
	private val defaultChipTextColor: ColorStateList
	private val defaultChipIconTint: ColorStateList
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
		@SuppressLint("CustomViewStyleable")
		val a = context.obtainStyledAttributes(null, materialR.styleable.Chip, 0, R.style.Widget_Kotatsu_Chip)
		defaultChipStrokeColor = a.getColorStateListOrThrow(materialR.styleable.Chip_chipStrokeColor)
		defaultChipTextColor = a.getColorStateListOrThrow(materialR.styleable.Chip_android_textColor)
		defaultChipIconTint = a.getColorStateListOrThrow(materialR.styleable.Chip_chipIconTint)
		a.recycle()
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
		chip.text = model.title
		val tint = if (model.tint == 0) {
			null
		} else {
			ContextCompat.getColorStateList(context, model.tint)
		}
		chip.chipIconTint = tint ?: defaultChipIconTint
		chip.checkedIconTint = tint ?: defaultChipIconTint
		chip.chipStrokeColor = tint ?: defaultChipStrokeColor
		chip.setTextColor(tint ?: defaultChipTextColor)
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
		chip.tag = model.data
	}

	private fun addChip(): Chip {
		val chip = Chip(context)
		val drawable = ChipDrawable.createFromAttributes(context, null, 0, R.style.Widget_Kotatsu_Chip)
		chip.setChipDrawable(drawable)
		chip.isCheckedIconVisible = true
		chip.isChipIconVisible = false
		chip.setCheckedIconResource(R.drawable.ic_check)
		chip.checkedIconTint = defaultChipIconTint
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
		@ColorRes val tint: Int,
		val title: CharSequence,
		@DrawableRes val icon: Int,
		val isCheckable: Boolean,
		val isChecked: Boolean,
		val data: Any? = null,
	)

	fun interface OnChipClickListener {

		fun onChipClick(chip: Chip, data: Any?)
	}

	fun interface OnChipCloseClickListener {

		fun onChipCloseClick(chip: Chip, data: Any?)
	}
}
