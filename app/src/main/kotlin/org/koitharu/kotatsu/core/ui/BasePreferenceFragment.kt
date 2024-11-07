package org.koitharu.kotatsu.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.get
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.ui.util.WindowInsetsDelegate
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.getThemeDrawable
import org.koitharu.kotatsu.core.util.ext.parentView
import org.koitharu.kotatsu.settings.SettingsActivity
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(),
	WindowInsetsDelegate.WindowInsetsListener,
	RecyclerViewOwner,
	ExceptionResolver.Host {

	protected lateinit var exceptionResolver: ExceptionResolver
		private set

	@Inject
	lateinit var settings: AppSettings

	@JvmField
	protected val insetsDelegate = WindowInsetsDelegate()

	override val recyclerView: RecyclerView
		get() = listView

	override fun onAttach(context: Context) {
		super.onAttach(context)
		val entryPoint = EntryPointAccessors.fromApplication<BaseActivityEntryPoint>(context)
		exceptionResolver = entryPoint.exceptionResolverFactory.create(this)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val themedContext = (view.parentView ?: view).context
		view.setBackgroundColor(themedContext.getThemeColor(android.R.attr.colorBackground))
		listView.clipToPadding = false
		insetsDelegate.onViewCreated(view)
		insetsDelegate.addInsetsListener(this)
	}

	override fun onDestroyView() {
		insetsDelegate.removeInsetsListener(this)
		insetsDelegate.onDestroyView()
		super.onDestroyView()
	}

	override fun onResume() {
		super.onResume()
		setTitle(if (titleId != 0) getString(titleId) else null)
		arguments?.getString(SettingsActivity.ARG_PREF_KEY)?.let {
			focusPreference(it)
			arguments?.remove(SettingsActivity.ARG_PREF_KEY)
		}
	}

	@CallSuper
	override fun onWindowInsetsChanged(insets: Insets) {
		listView.updatePadding(
			bottom = insets.bottom,
		)
	}

	protected open fun setTitle(title: CharSequence?) {
		(activity as? SettingsActivity)?.setSectionTitle(title)
	}

	protected fun startActivitySafe(intent: Intent): Boolean = try {
		startActivity(intent)
		true
	} catch (_: ActivityNotFoundException) {
		Snackbar.make(listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
		false
	}

	private fun focusPreference(key: String) {
		val pref = findPreference<Preference>(key)
		if (pref == null) {
			scrollToPreference(key)
			return
		}
		scrollToPreference(pref)
		val prefIndex = preferenceScreen.indexOf(key)
		val view = if (prefIndex >= 0) {
			listView.findViewHolderForAdapterPosition(prefIndex)?.itemView ?: return
		} else {
			return
		}
		view.context.getThemeDrawable(materialR.attr.colorTertiaryContainer)?.let {
			view.background = it
		}
	}

	private fun PreferenceScreen.indexOf(key: String): Int {
		for (i in 0 until preferenceCount) {
			if (get(i).key == key) {
				return i
			}
		}
		return -1
	}
}
