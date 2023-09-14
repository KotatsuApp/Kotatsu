package org.koitharu.kotatsu.reader.ui.pager.standard

import android.graphics.PointF
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnGenericMotionListener
import android.view.animation.DecelerateInterpolator
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class SsivZoomListener : OnGenericMotionListener {

	override fun onGenericMotion(v: View?, event: MotionEvent): Boolean {
		val ssiv = v as? SubsamplingScaleImageView ?: return false
		if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
			if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
				val axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
				val withCtrl = event.metaState and KeyEvent.META_CTRL_MASK != 0
				if (withCtrl || ssiv.scale > ssiv.minScale) {
					val center = PointF(event.x, event.y)
					val scale = ssiv.scale + axisValue * 1.6f
					(ssiv.animateScaleAndCenter(scale, center) ?: return false)
						.withInterpolator(DecelerateInterpolator())
						.start()
					return true
				}
			}
		}
		return false
	}
}
