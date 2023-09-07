package org.koitharu.kotatsu.core.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

@SuppressLint("ClickableViewAccessibility")
class EnhancedViewPager @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : ViewPager(context, attrs) {

	var isUserInputEnabled: Boolean = true
		set(value) {
			field = value
			if (!value) {
				cancelPendingInputEvents()
			}
		}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		return isUserInputEnabled && super.onTouchEvent(event)
	}

	override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
		return try {
			isUserInputEnabled && super.onInterceptTouchEvent(event)
		} catch (e: IllegalArgumentException) {
			e.printStackTraceDebug()
			false
		}
	}
}
