package org.koitharu.kotatsu.details.ui

import android.text.Spannable
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

class AuthorSpan(private val listener: OnAuthorClickListener) : ClickableSpan() {

	override fun onClick(widget: View) {
		val text = (widget as? TextView)?.text as? Spannable ?: return
		val start = text.getSpanStart(this)
		val end = text.getSpanEnd(this)
		val selected = text.substring(start, end).trim()
		if (selected.isNotEmpty()) {
			listener.onAuthorClick(selected)
		}
	}

	override fun updateDrawState(ds: TextPaint) {
		ds.setColor(ds.linkColor)
	}

	fun interface OnAuthorClickListener {

		fun onAuthorClick(author: String)
	}
}
