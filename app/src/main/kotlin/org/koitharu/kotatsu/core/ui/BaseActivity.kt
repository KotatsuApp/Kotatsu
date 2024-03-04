package org.koitharu.kotatsu.core.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.ActionBarContextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.EntryPointAccessors
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.ui.util.ActionModeDelegate
import org.koitharu.kotatsu.core.ui.util.BaseActivityEntryPoint
import org.koitharu.kotatsu.core.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.isWebViewUnavailable

@Suppress("LeakingThis")
abstract class BaseActivity<B : ViewBinding> :
	AppCompatActivity(),
	WindowInsetsDelegate.WindowInsetsListener {

	private var isAmoledTheme = false

	lateinit var viewBinding: B
		private set

	@JvmField
	protected val exceptionResolver = ExceptionResolver(this)

	@JvmField
	protected val insetsDelegate = WindowInsetsDelegate()

	@JvmField
	val actionModeDelegate = ActionModeDelegate()

	private var defaultStatusBarColor = Color.TRANSPARENT

	override fun onCreate(savedInstanceState: Bundle?) {
		val settings = EntryPointAccessors.fromApplication(this, BaseActivityEntryPoint::class.java).settings
		isAmoledTheme = settings.isAmoledTheme
		setTheme(settings.colorScheme.styleResId)
		if (isAmoledTheme) {
			setTheme(R.style.ThemeOverlay_Kotatsu_Amoled)
		}
		putDataToExtras(intent)
		super.onCreate(savedInstanceState)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		insetsDelegate.handleImeInsets = true
		insetsDelegate.addInsetsListener(this)
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		onBackPressedDispatcher.addCallback(actionModeDelegate)
	}

	override fun onNewIntent(intent: Intent?) {
		putDataToExtras(intent)
		super.onNewIntent(intent)
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
		this.viewBinding = binding
		super.setContentView(binding.root)
		val toolbar = (binding.root.findViewById<View>(R.id.toolbar) as? Toolbar)
		toolbar?.let(this::setSupportActionBar)
		insetsDelegate.onViewCreated(binding.root)
	}

	override fun onSupportNavigateUp(): Boolean {
		dispatchNavigateUp()
		return true
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
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
		return isNight && isAmoledTheme
	}

	@CallSuper
	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		actionModeDelegate.onSupportActionModeStarted(mode)
		val actionModeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			ColorUtils.compositeColors(
				ContextCompat.getColor(this, com.google.android.material.R.color.m3_appbar_overlay_color),
				getThemeColor(R.attr.m3ColorBackground),
			)
		} else {
			ContextCompat.getColor(this, R.color.kotatsu_m3_background)
		}
		val insets = ViewCompat.getRootWindowInsets(viewBinding.root)
			?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: return
		findViewById<ActionBarContextView?>(androidx.appcompat.R.id.action_mode_bar).apply {
			setBackgroundColor(actionModeColor)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
		defaultStatusBarColor = window.statusBarColor
		window.statusBarColor = actionModeColor
	}

	@CallSuper
	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		actionModeDelegate.onSupportActionModeFinished(mode)
		window.statusBarColor = defaultStatusBarColor
	}

	protected open fun dispatchNavigateUp() {
		val upIntent = parentActivityIntent
		if (upIntent != null) {
			if (!navigateUpTo(upIntent)) {
				startActivity(upIntent)
			}
		} else {
			finishAfterTransition()
		}
	}

	private fun putDataToExtras(intent: Intent?) {
		intent?.putExtra(EXTRA_DATA, intent.data)
	}

	protected fun setContentViewWebViewSafe(viewBindingProducer: () -> B): Boolean {
		return try {
			setContentView(viewBindingProducer())
			true
		} catch (e: Exception) {
			if (e.isWebViewUnavailable()) {
				Toast.makeText(this, R.string.web_view_unavailable, Toast.LENGTH_LONG).show()
				finishAfterTransition()
				false
			} else {
				throw e
			}
		}
	}

	companion object {

		const val EXTRA_DATA = "data"
	}
}
