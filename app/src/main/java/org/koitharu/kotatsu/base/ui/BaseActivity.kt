package org.koitharu.kotatsu.base.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.ActionBarContextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.util.ActionModeDelegate
import org.koitharu.kotatsu.base.ui.util.BaseActivityEntryPoint
import org.koitharu.kotatsu.base.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.base.ui.util.inject
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.ext.getThemeColor

abstract class BaseActivity<B : ViewBinding> :
	AppCompatActivity(),
	WindowInsetsDelegate.WindowInsetsListener {

	@Inject
	lateinit var settings: AppSettings

	protected lateinit var binding: B
		private set

	@Suppress("LeakingThis")
	protected val exceptionResolver = ExceptionResolver(this)

	@Suppress("LeakingThis")
	protected val insetsDelegate = WindowInsetsDelegate(this)

	val actionModeDelegate = ActionModeDelegate()

	override fun onCreate(savedInstanceState: Bundle?) {
		EntryPointAccessors.fromApplication(this, BaseActivityEntryPoint::class.java).inject(this)
		val isAmoled = settings.isAmoledTheme
		val isDynamic = settings.isDynamicTheme
		// TODO support DialogWhenLarge theme
		when {
			isAmoled && isDynamic -> setTheme(R.style.Theme_Kotatsu_Monet_Amoled)
			isAmoled -> setTheme(R.style.Theme_Kotatsu_Amoled)
			isDynamic -> setTheme(R.style.Theme_Kotatsu_Monet)
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
			// ActivityCompat.recreate(this)
			TODO("Test error")
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
		return isNight && settings.isAmoledTheme
	}

	@CallSuper
	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		actionModeDelegate.onSupportActionModeStarted(mode)
		val actionModeColor = ColorUtils.compositeColors(
			ContextCompat.getColor(this, com.google.android.material.R.color.m3_appbar_overlay_color),
			getThemeColor(com.google.android.material.R.attr.colorSurface),
		)
		val insets = ViewCompat.getRootWindowInsets(binding.root)
			?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: return
		findViewById<ActionBarContextView?>(androidx.appcompat.R.id.action_mode_bar).apply {
			setBackgroundColor(actionModeColor)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
		window.statusBarColor = actionModeColor
	}

	@CallSuper
	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		actionModeDelegate.onSupportActionModeFinished(mode)
		window.statusBarColor = getThemeColor(android.R.attr.statusBarColor)
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
