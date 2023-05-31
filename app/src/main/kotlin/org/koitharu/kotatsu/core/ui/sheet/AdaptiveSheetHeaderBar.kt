package org.koitharu.kotatsu.core.ui.sheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.parents
import org.koitharu.kotatsu.databinding.LayoutSheetHeaderAdaptiveBinding

class AdaptiveSheetHeaderBar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), AdaptiveSheetCallback {

	private val binding = LayoutSheetHeaderAdaptiveBinding.inflate(LayoutInflater.from(context), this)
	private var sheetBehavior: AdaptiveSheetBehavior? = null

	var title: CharSequence?
		get() = binding.shTextViewTitle.text
		set(value) {
			binding.shTextViewTitle.text = value
		}

	val isTitleVisible: Boolean
		get() = binding.shLayoutSidesheet.isVisible

	init {
		orientation = VERTICAL
		binding.shButtonClose.setOnClickListener { dismissSheet() }
		context.withStyledAttributes(
			attrs,
			R.styleable.AdaptiveSheetHeaderBar, defStyleAttr,
		) {
			title = getText(R.styleable.AdaptiveSheetHeaderBar_title)
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		if (isInEditMode) {
			val isTabled = resources.getBoolean(R.bool.is_tablet)
			binding.shDragHandle.isGone = isTabled
			binding.shLayoutSidesheet.isVisible = isTabled
		} else {
			setBottomSheetBehavior(findParentSheetBehavior())
		}
	}

	override fun onDetachedFromWindow() {
		setBottomSheetBehavior(null)
		super.onDetachedFromWindow()
	}

	override fun onStateChanged(sheet: View, newState: Int) {

	}

	fun setTitle(@StringRes resId: Int) {
		binding.shTextViewTitle.setText(resId)
	}

	private fun setBottomSheetBehavior(behavior: AdaptiveSheetBehavior?) {
		binding.shDragHandle.isVisible = behavior is AdaptiveSheetBehavior.Bottom
		binding.shLayoutSidesheet.isVisible = behavior is AdaptiveSheetBehavior.Side
		sheetBehavior?.removeCallback(this)
		sheetBehavior = behavior
		behavior?.addCallback(this)
	}

	private fun dismissSheet() {
		sheetBehavior?.state = AdaptiveSheetBehavior.STATE_HIDDEN
	}

	private fun findParentSheetBehavior(): AdaptiveSheetBehavior? {
		for (p in parents) {
			val layoutParams = (p as? View)?.layoutParams
			if (layoutParams is CoordinatorLayout.LayoutParams) {
				AdaptiveSheetBehavior.from(layoutParams)?.let {
					return it
				}
			}
		}
		return null
	}
}
