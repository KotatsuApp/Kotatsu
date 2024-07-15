package org.koitharu.kotatsu.details.ui

import android.annotation.SuppressLint
import android.transition.TransitionManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.widgets.SelectableTextView
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled

@SuppressLint("ClickableViewAccessibility")
class TitleExpandListener(
	private val textView: SelectableTextView,
) : GestureDetector.SimpleOnGestureListener(), OnTouchListener {

	private val gestureDetector = GestureDetector(textView.context, this)
	private val linesExpanded = textView.resources.getInteger(R.integer.details_description_lines)
	private val linesCollapsed = textView.resources.getInteger(R.integer.details_title_lines)

	override fun onTouch(v: View?, event: MotionEvent) = gestureDetector.onTouchEvent(event)

	override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
		if (textView.context.isAnimationsEnabled) {
			TransitionManager.beginDelayedTransition(textView.parent as ViewGroup)
		}
		if (textView.maxLines in 1 until Integer.MAX_VALUE) {
			textView.maxLines = Integer.MAX_VALUE
		} else {
			textView.maxLines = linesCollapsed
		}
		return true
	}

	override fun onLongPress(e: MotionEvent) {
		textView.maxLines = Integer.MAX_VALUE
		textView.selectAll()
	}

	fun attach() {
		textView.setOnTouchListener(this)
	}
}
