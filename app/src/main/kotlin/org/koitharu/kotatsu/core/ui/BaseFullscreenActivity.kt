package org.koitharu.kotatsu.core.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.R

abstract class BaseFullscreenActivity<B : ViewBinding> :
	BaseActivity<B>() {

	private lateinit var insetsControllerCompat: WindowInsetsControllerCompat

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		with(window) {
			insetsControllerCompat = WindowInsetsControllerCompat(this, decorView)
			statusBarColor = Color.TRANSPARENT
			navigationBarColor = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				ContextCompat.getColor(this@BaseFullscreenActivity, R.color.dim)
			} else {
				Color.TRANSPARENT
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				attributes.layoutInDisplayCutoutMode =
					WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
			}
		}
		insetsControllerCompat.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		showSystemUI()
	}

	protected fun hideSystemUI() {
		insetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())
	}

	protected fun showSystemUI() {
		insetsControllerCompat.show(WindowInsetsCompat.Type.systemBars())
	}
}
