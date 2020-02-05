package org.koitharu.kotatsu.ui.common

import android.os.Bundle
import android.view.View

abstract class BaseFullscreenActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
			if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
				onSystemUiShown()
			} else {
				onSystemUiHidden()
			}
		}
	}

	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		if (hasFocus) hideSystemUI()
	}

	protected fun hideSystemUI() {
		window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				or View.SYSTEM_UI_FLAG_FULLSCREEN)
	}

	protected fun showSystemUI() {
		window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
	}

	protected open fun onSystemUiShown() = Unit

	protected open fun onSystemUiHidden() = Unit
}