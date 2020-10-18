package org.koitharu.kotatsu.ui.base

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager


abstract class BaseFullscreenActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		with(window) {
			statusBarColor = Color.TRANSPARENT
			navigationBarColor = Color.TRANSPARENT
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				attributes.layoutInDisplayCutoutMode =
					WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
			}
		}
		showSystemUI()
	}

	protected fun hideSystemUI() {
		window.decorView.systemUiVisibility = (
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // прячем панель навигации
						or View.SYSTEM_UI_FLAG_FULLSCREEN // прячем строку состояния
						or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				)

	}

	protected fun showSystemUI() {
		window.decorView.systemUiVisibility =
			(View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
	}
}