package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class TouchBlockLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var isTouchEventsAllowed = true

    override fun onInterceptTouchEvent(
        ev: MotionEvent?
    ): Boolean = if (isTouchEventsAllowed) {
        super.onInterceptTouchEvent(ev)
    } else {
        true
    }
}
