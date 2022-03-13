package org.koitharu.kotatsu.main.ui

import android.view.View
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.lang.ref.WeakReference
import com.google.android.material.R as materialR

class NavigationViewInsetsListener : OnApplyWindowInsetsListener {

	private var menuViewRef: WeakReference<View>? = null

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val menuView = menuViewRef?.get() ?: v.findViewById<View>(materialR.id.design_navigation_view).also {
			menuViewRef = WeakReference(it)
		}
		val systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		v.updatePadding(top = systemWindowInsets.top)
		// NavigationView doesn't dispatch insets to the menu view, so pad the bottom here.
		menuView.updatePadding(bottom = systemWindowInsets.bottom)
		return WindowInsetsCompat.CONSUMED
	}
}