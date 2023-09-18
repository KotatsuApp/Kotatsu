package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ViewZoomBinding

class ZoomControl @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : LinearLayout(context, attrs), View.OnClickListener {

	private val binding = ViewZoomBinding.inflate(LayoutInflater.from(context), this)

	var listener: ZoomControlListener? = null

	init {
		binding.buttonZoomIn.setOnClickListener(this)
		binding.buttonZoomOut.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_zoom_in -> listener?.onZoomIn()
			R.id.button_zoom_out -> listener?.onZoomOut()
		}
	}

	interface ZoomControlListener {

		fun onZoomIn()

		fun onZoomOut()
	}
}
