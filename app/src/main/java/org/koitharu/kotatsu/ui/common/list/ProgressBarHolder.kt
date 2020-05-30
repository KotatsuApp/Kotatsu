package org.koitharu.kotatsu.ui.common.list

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_progress.*
import org.koitharu.kotatsu.R

class ProgressBarHolder(parent: ViewGroup) :
	BaseViewHolder<Boolean, Unit>(parent, R.layout.item_progress) {

	override fun onBind(data: Boolean, extra: Unit) {
		progressBar.visibility = if (data) {
			View.VISIBLE
		} else {
			View.INVISIBLE
		}
	}
}