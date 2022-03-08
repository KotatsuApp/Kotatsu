package org.koitharu.kotatsu.base.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.viewbinding.ViewBinding

private const val SYSTEM_UI_FLAGS_SHOWN = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
	View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
	View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

private const val SYSTEM_UI_FLAGS_HIDDEN = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
	View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
	View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
	View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
	View.SYSTEM_UI_FLAG_FULLSCREEN or
	View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

abstract class BaseFullscreenActivity<B : ViewBinding> : BaseActivity<B>(),
	View.OnSystemUiVisibilityChangeListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		with(window) {
			statusBarColor = Color.TRANSPARENT
			navigationBarColor = Color.TRANSPARENT
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				attributes.layoutInDisplayCutoutMode =
					WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
			}
			decorView.setOnSystemUiVisibilityChangeListener(this@BaseFullscreenActivity)
		}
		showSystemUI()
	}

	@Deprecated("Deprecated in Java")
	final override fun onSystemUiVisibilityChange(visibility: Int) {
		onSystemUiVisibilityChanged(visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0)
	}

	// TODO WindowInsetsControllerCompat works incorrect
	protected fun hideSystemUI() {
		window.decorView.systemUiVisibility = SYSTEM_UI_FLAGS_HIDDEN
	}

	protected fun showSystemUI() {
		window.decorView.systemUiVisibility = SYSTEM_UI_FLAGS_SHOWN
	}

	protected open fun onSystemUiVisibilityChanged(isVisible: Boolean) = Unit
}