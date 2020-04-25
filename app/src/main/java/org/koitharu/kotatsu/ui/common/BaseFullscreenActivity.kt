package org.koitharu.kotatsu.ui.common

import android.os.Build
import android.os.Bundle
import android.view.View


abstract class BaseFullscreenActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		showSystemUI()
	}

	protected fun hideSystemUI() {
		window.decorView.systemUiVisibility =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				(
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // прячем панель навигации
								or View.SYSTEM_UI_FLAG_FULLSCREEN // прячем строку состояния
								or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
						)
			} else {
				(
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // прячем панель навигации
								or View.SYSTEM_UI_FLAG_FULLSCREEN // прячем строку состояния
						)
			}

	}

	protected fun showSystemUI() {
		window.decorView.systemUiVisibility =
			(View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
	}
}