package org.koitharu.kotatsu.list.ui.size

import android.view.View
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.history.ui.util.ReadingProgressView

class StaticItemSizeResolver(
	override val cellWidth: Int,
) : ItemSizeResolver {

	private var widthThreshold: Int = -1
	private var textAppearanceResId = R.style.TextAppearance_Kotatsu_GridTitle

	override fun attachToView(
		lifecycleOwner: LifecycleOwner,
		view: View,
		textView: TextView?,
		progressView: ReadingProgressView?
	) {
		if (widthThreshold == -1) {
			widthThreshold = view.resources.getDimensionPixelSize(R.dimen.small_grid_width)
			textAppearanceResId = if (cellWidth < widthThreshold) {
				R.style.TextAppearance_Kotatsu_GridTitle_Small
			} else {
				R.style.TextAppearance_Kotatsu_GridTitle
			}
		}
		if (textView != null) {
			TextViewCompat.setTextAppearance(textView, textAppearanceResId)
		}
		view.updateLayoutParams {
			width = cellWidth
		}
		progressView?.adjustSize()
	}

	private fun ReadingProgressView.adjustSize() {
		val lp = layoutParams
		val size = resources.getDimensionPixelSize(
			if (cellWidth < widthThreshold) {
				R.dimen.card_indicator_size_small
			} else {
				R.dimen.card_indicator_size
			},
		)
		if (lp.width != size || lp.height != size) {
			lp.width = size
			lp.height = size
			layoutParams = lp
		}
	}
}
