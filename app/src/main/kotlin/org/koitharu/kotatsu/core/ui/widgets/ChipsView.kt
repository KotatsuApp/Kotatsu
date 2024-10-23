package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.children
import coil3.ImageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.image.ChipIconTarget
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.setProgressIcon
import org.koitharu.kotatsu.parsers.util.ifZero
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ChipsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = com.google.android.material.R.attr.chipGroupStyle,
) : ChipGroup(context, attrs, defStyleAttr) {

	@Inject
	lateinit var coil: ImageLoader

	private var isLayoutSuppressedCompat = false
	private var isLayoutCalledOnSuppressed = false
	private val chipOnClickListener = InternalChipClickListener()
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
				val chip = getChildAt(i) as DataChip? ?: addChip()
				chip.bind(model)
			}
			if (childCount > items.size) {
				removeViews(items.size, childCount - items.size)
			}
		} finally {
			suppressLayoutCompat(false)
		}
	}

	private fun addChip() = DataChip(context).also { addView(it) }

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
		val iconData: Any? = null,
		@ColorRes val tint: Int = 0,
		val isChecked: Boolean = false,
		val isLoading: Boolean = false,
		val isDropdown: Boolean = false,
		val isCloseable: Boolean = false,
		val data: Any? = null,
	)

	private inner class DataChip(context: Context) : Chip(context) {

		private var model: ChipModel? = null
		private var imageRequest: Disposable? = null

		init {
			val drawable = ChipDrawable.createFromAttributes(context, null, 0, chipStyle)
			setChipDrawable(drawable)
			isChipIconVisible = false
			setOnCloseIconClickListener(chipOnCloseListener)
			setEnsureMinTouchTargetSize(false)
			setOnClickListener(chipOnClickListener)
			isElegantTextHeight = false
		}

		fun bind(model: ChipModel) {
			if (this.model == model) {
				return
			}
			this.model = model

			if (model.titleResId == 0) {
				text = model.title
			} else {
				setText(model.titleResId)
			}
			isClickable = onChipClickListener != null
			if (model.isChecked) {
				isCheckable = true
				isChecked = true
			} else {
				isChecked = false
				isCheckable = false
			}
			bindIcon(model)
			isCheckedIconVisible = model.isChecked
			isCloseIconVisible = if (model.isCloseable || model.isDropdown) {
				setCloseIconResource(
					if (model.isDropdown) R.drawable.ic_expand_more else materialR.drawable.ic_m3_chip_close,
				)
				true
			} else {
				false
			}
			tag = model.data
		}

		override fun toggle() = Unit

		private fun bindIcon(model: ChipModel) {
			when {
				model.isChecked -> {
					imageRequest?.dispose()
					imageRequest = null
					chipIcon = null
					isChipIconVisible = false
				}

				model.isLoading -> {
					imageRequest?.dispose()
					imageRequest = null
					isChipIconVisible = true
					setProgressIcon()
				}

				model.iconData != null -> {
					val placeholder = model.icon.ifZero { materialR.drawable.navigation_empty_icon }
					imageRequest = ImageRequest.Builder(context)
						.data(model.iconData)
						.crossfade(false)
						.size(resources.getDimensionPixelSize(materialR.dimen.m3_chip_icon_size))
						.target(ChipIconTarget(this))
						.placeholder(placeholder)
						.fallback(placeholder)
						.error(placeholder)
						.transformations(RoundedCornersTransformation(resources.getDimension(R.dimen.chip_icon_corner)))
						.allowRgb565(true)
						.enqueueWith(coil)
					isChipIconVisible = true
				}

				model.icon != 0 -> {
					imageRequest?.dispose()
					imageRequest = null
					setChipIconResource(model.icon)
					isChipIconVisible = true
				}

				else -> {
					imageRequest?.dispose()
					imageRequest = null
					chipIcon = null
					isChipIconVisible = false
				}
			}
		}
	}

	private inner class InternalChipClickListener : OnClickListener {
		override fun onClick(v: View?) {
			val chip = v as? DataChip ?: return
			onChipClickListener?.onChipClick(chip, chip.tag)
		}
	}

	fun interface OnChipClickListener {

		fun onChipClick(chip: Chip, data: Any?)
	}

	fun interface OnChipCloseClickListener {

		fun onChipCloseClick(chip: Chip, data: Any?)
	}
}
