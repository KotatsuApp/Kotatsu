package org.koitharu.kotatsu.core.ui.util

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.shape.MaterialShapeDrawable
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import com.google.android.material.R as materialR

class StatusBarDimHelper : AppBarLayout.OnOffsetChangedListener {

	private var animator: ValueAnimator? = null
	private val interpolator = AccelerateDecelerateInterpolator()

	override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
		val foreground = appBarLayout.statusBarForeground ?: return
		val start = foreground.alpha
		val collapsed = verticalOffset != 0
		val end = if (collapsed) 255 else 0
		animator?.cancel()
		if (start == end) {
			animator = null
			return
		}
		animator = ValueAnimator.ofInt(start, end).apply {
			duration = appBarLayout.context.getAnimationDuration(materialR.integer.app_bar_elevation_anim_duration)
			interpolator = this@StatusBarDimHelper.interpolator
			addUpdateListener {
				foreground.alpha = it.animatedValue as Int
			}
			start()
		}
	}

	fun attachToAppBar(appBarLayout: AppBarLayout) {
		appBarLayout.addOnOffsetChangedListener(this)
		appBarLayout.statusBarForeground =
			MaterialShapeDrawable.createWithElevationOverlay(appBarLayout.context).apply {
				alpha = 0
			}
	}
}
