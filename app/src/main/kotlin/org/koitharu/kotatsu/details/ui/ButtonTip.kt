package org.koitharu.kotatsu.details.ui

import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.Insets
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.util.ext.getThemeDimensionPixelSize
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.databinding.ItemTipBinding
import com.google.android.material.R as materialR

class ButtonTip(
	private val root: ViewGroup,
	private val insetsDelegate: WindowInsetsDelegate,
	private val viewModel: DetailsViewModel,
) : View.OnClickListener, WindowInsetsDelegate.WindowInsetsListener {

	private var selfBinding = ItemTipBinding.inflate(LayoutInflater.from(root.context), root, false)
	private val actionBarSize = root.context.getThemeDimensionPixelSize(materialR.attr.actionBarSize)

	init {
		selfBinding.textView.setText(R.string.details_button_tip)
		selfBinding.imageViewIcon.setImageResource(R.drawable.ic_tap)
		selfBinding.root.id = R.id.layout_tip
		selfBinding.buttonClose.setOnClickListener(this)
	}

	override fun onClick(v: View?) {
		remove()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		if (root is CoordinatorLayout) {
			selfBinding.root.updateLayoutParams<CoordinatorLayout.LayoutParams> {
				bottomMargin = topMargin + insets.bottom + insets.top + actionBarSize
			}
		}
	}

	fun addToRoot() {
		val lp: ViewGroup.LayoutParams = when (root) {
			is CoordinatorLayout -> CoordinatorLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			).apply {
				// anchorId = R.id.layout_bottom
				// anchorGravity = Gravity.TOP
				gravity = Gravity.BOTTOM
				setMargins(root.resources.getDimensionPixelOffset(R.dimen.margin_normal))
				bottomMargin += actionBarSize
			}

			is ConstraintLayout -> ConstraintLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			).apply {
				width = root.resources.getDimensionPixelSize(R.dimen.m3_side_sheet_width)
				setMargins(root.resources.getDimensionPixelOffset(R.dimen.margin_normal))
			}

			else -> ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		}
		root.addView(selfBinding.root, lp)
		if (root is ConstraintLayout) {
			val cs = ConstraintSet()
			cs.clone(root)
			cs.connect(R.id.layout_tip, ConstraintSet.TOP, R.id.appbar, ConstraintSet.BOTTOM)
			cs.connect(R.id.layout_tip, ConstraintSet.START, R.id.card_chapters, ConstraintSet.START)
			cs.connect(R.id.layout_tip, ConstraintSet.END, R.id.card_chapters, ConstraintSet.END)
			cs.applyTo(root)
		}
		insetsDelegate.addInsetsListener(this)
	}

	fun remove() {
		if (root.context.isAnimationsEnabled) {
			TransitionManager.beginDelayedTransition(root)
		}
		insetsDelegate.removeInsetsListener(this)
		root.removeView(selfBinding.root)
		viewModel.onButtonTipClosed()
	}
}
