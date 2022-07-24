package org.koitharu.kotatsu.list.ui

import android.content.SharedPreferences
import android.content.res.Resources
import android.view.View
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.roundToInt
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings

class ItemSizeResolver(resources: Resources, private val settings: AppSettings) {

	private val gridWidth = resources.getDimension(R.dimen.preferred_grid_width)
	private val scaleFactor: Float
		get() = settings.gridSize / 100f

	val cellWidth: Int
		get() = (gridWidth * scaleFactor).roundToInt()

	fun attachToView(lifecycleOwner: LifecycleOwner, view: View, textView: TextView?) {
		val observer = SizeObserver(view, textView)
		view.addOnAttachStateChangeListener(observer)
		lifecycleOwner.lifecycle.addObserver(observer)
		if (view.isAttachedToWindow) {
			observer.update()
		}
	}

	private inner class SizeObserver(
		private val view: View,
		private val textView: TextView?,
	) : DefaultLifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener, View.OnAttachStateChangeListener {

		private val widthThreshold = view.resources.getDimensionPixelSize(R.dimen.small_grid_width)

		@StyleRes
		private var prevTextAppearance = 0

		override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
			if (key == AppSettings.KEY_GRID_SIZE) {
				update()
			}
		}

		override fun onViewAttachedToWindow(v: View?) {
			settings.subscribe(this)
			update()
		}

		override fun onViewDetachedFromWindow(v: View?) {
			settings.unsubscribe(this)
		}

		override fun onDestroy(owner: LifecycleOwner) {
			super.onDestroy(owner)
			settings.unsubscribe(this)
			view.removeOnAttachStateChangeListener(this)
		}

		fun update() {
			val newWidth = cellWidth
			textView?.adjustTextAppearance(newWidth)
			view.updateLayoutParams {
				width = newWidth
			}
		}

		private fun TextView.adjustTextAppearance(width: Int) {
			val textAppearanceResId = if (width < widthThreshold) {
				R.style.TextAppearance_Kotatsu_GridTitle_Small
			} else {
				R.style.TextAppearance_Kotatsu_GridTitle
			}
			if (textAppearanceResId != prevTextAppearance) {
				prevTextAppearance = textAppearanceResId
				setTextAppearance(textAppearanceResId)
				requestLayout()
			}
		}
	}
}
