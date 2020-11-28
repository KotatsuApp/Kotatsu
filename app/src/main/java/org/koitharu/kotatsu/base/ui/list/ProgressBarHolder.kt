package org.koitharu.kotatsu.base.ui.list

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_progress.*
import org.koitharu.kotatsu.R

class ProgressBarHolder(parent: ViewGroup) :
	BaseViewHolder<Boolean, Unit>(parent, R.layout.item_progress) {

	private var pendingVisibility: Int = View.GONE
	private val action = Runnable {
		progressBar?.visibility = pendingVisibility
		pendingVisibility = View.GONE
	}

	override fun onBind(data: Boolean, extra: Unit) {
		val visibility = if (data) {
			View.VISIBLE
		} else {
			View.INVISIBLE
		}
		if (visibility != progressBar.visibility && visibility != pendingVisibility) {
			progressBar.removeCallbacks(action)
			pendingVisibility = visibility
			progressBar.postDelayed(action, 400)
		}
	}

	override fun onRecycled() {
		progressBar.removeCallbacks(action)
		super.onRecycled()
	}
}