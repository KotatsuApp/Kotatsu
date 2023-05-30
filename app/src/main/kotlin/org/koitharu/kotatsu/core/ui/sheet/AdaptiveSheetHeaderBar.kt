package org.koitharu.kotatsu.core.ui.sheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
		get() = binding.textViewTitle.text
		set(value) {
			binding.textViewTitle.text = value
		}

	val isExpanded: Boolean
		get() = binding.dragHandle.isGone

	init {
		orientation = VERTICAL
		binding.buttonClose.setOnClickListener { dismissSheet() }
		context.withStyledAttributes(
			attrs,
			R.styleable.AdaptiveSheetHeaderBar, defStyleAttr,
		) {
			title = getText(R.styleable.AdaptiveSheetHeaderBar_title)
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		dispatchInsets(ViewCompat.getRootWindowInsets(this))
		setBottomSheetBehavior(findParentSheetBehavior())
	}

	override fun onDetachedFromWindow() {
		setBottomSheetBehavior(null)
		super.onDetachedFromWindow()
	}

	override fun onApplyWindowInsets(insets: WindowInsets?): WindowInsets {
		dispatchInsets(if (insets != null) WindowInsetsCompat.toWindowInsetsCompat(insets) else null)
		return super.onApplyWindowInsets(insets)
	}

	override fun onStateChanged(sheet: View, newState: Int) {

	}

	fun setTitle(@StringRes resId: Int) {
		binding.textViewTitle.setText(resId)
	}

	private fun dispatchInsets(insets: WindowInsetsCompat?) {

	}

	private fun setBottomSheetBehavior(behavior: AdaptiveSheetBehavior?) {
		binding.dragHandle.isVisible = behavior is AdaptiveSheetBehavior.Bottom
		binding.layoutSidesheet.isVisible = behavior is AdaptiveSheetBehavior.Side
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
