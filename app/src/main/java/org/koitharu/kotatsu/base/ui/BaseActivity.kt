package org.koitharu.kotatsu.base.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.ActionBarContextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.AppSettings

abstract class BaseActivity<B : ViewBinding> : AppCompatActivity(),
	WindowInsetsDelegate.WindowInsetsListener {

	protected lateinit var binding: B
		private set

	@Suppress("LeakingThis")
	protected val exceptionResolver = ExceptionResolver(this)

	@Suppress("LeakingThis")
	protected val insetsDelegate = WindowInsetsDelegate(this)

	override fun onCreate(savedInstanceState: Bundle?) {
		val settings = get<AppSettings>()
		when {
			settings.isAmoledTheme -> setTheme(R.style.ThemeOverlay_Kotatsu_AMOLED)
			settings.isDynamicTheme -> setTheme(R.style.Theme_Kotatsu_Monet)
		}
		super.onCreate(savedInstanceState)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		insetsDelegate.handleImeInsets = true
	}

	@Deprecated("Use ViewBinding", level = DeprecationLevel.ERROR)
	override fun setContentView(layoutResID: Int) {
		super.setContentView(layoutResID)
		setupToolbar()
	}

	@Deprecated("Use ViewBinding", level = DeprecationLevel.ERROR)
	override fun setContentView(view: View?) {
		super.setContentView(view)
		setupToolbar()
	}

	protected fun setContentView(binding: B) {
		this.binding = binding
		super.setContentView(binding.root)
		val toolbar = (binding.root.findViewById<View>(R.id.toolbar) as? Toolbar)
		toolbar?.let(this::setSupportActionBar)
		insetsDelegate.onViewCreated(binding.root)
	}

	override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
		onBackPressed()
		true
	} else super.onOptionsItemSelected(item)

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_VOLUME_UP) { // TODO remove
			ActivityCompat.recreate(this)
			return true
		}
		return super.onKeyDown(keyCode, event)
	}

	private fun setupToolbar() {
		(findViewById<View>(R.id.toolbar) as? Toolbar)?.let(this::setSupportActionBar)
	}

	protected fun isDarkAmoledTheme(): Boolean {
		val uiMode = resources.configuration.uiMode
		val isNight = uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
		return isNight && get<AppSettings>().isAmoledTheme
	}

	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		val insets = ViewCompat.getRootWindowInsets(binding.root)
			?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: return
		val view = findViewById<ActionBarContextView?>(androidx.appcompat.R.id.action_mode_bar)
		view?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top
		}
	}

	override fun onBackPressed() {
		if ( // https://issuetracker.google.com/issues/139738913
			Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
			isTaskRoot &&
			supportFragmentManager.backStackEntryCount == 0
		) {
			finishAfterTransition()
		} else {
			super.onBackPressed()
		}
	}
}
