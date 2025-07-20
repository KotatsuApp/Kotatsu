package org.koitharu.kotatsu.core.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.get
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.container
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.getThemeDrawable
import org.koitharu.kotatsu.core.util.ext.parentView
import org.koitharu.kotatsu.core.util.ext.start
import org.koitharu.kotatsu.core.util.ext.systemBarsInsets
import org.koitharu.kotatsu.settings.SettingsActivity
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(),
	OnApplyWindowInsetsListener,
	RecyclerViewOwner,
	ExceptionResolver.Host {

	protected lateinit var exceptionResolver: ExceptionResolver
		private set

	@Inject
	lateinit var settings: AppSettings

	override val recyclerView: RecyclerView?
		get() = listView

	override fun onAttach(context: Context) {
		super.onAttach(context)
		val entryPoint = EntryPointAccessors.fromApplication<BaseActivityEntryPoint>(context)
		exceptionResolver = entryPoint.exceptionResolverFactory.create(this)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		ViewCompat.setOnApplyWindowInsetsListener(view, this)
		val themedContext = (view.parentView ?: view).context
		view.setBackgroundColor(themedContext.getThemeColor(android.R.attr.colorBackground))
		listView.clipToPadding = false
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		val isTablet = !resources.getBoolean(R.bool.is_tablet)
		val isMaster = container?.id == R.id.container_master
		listView.setPaddingRelative(
			if (isTablet && !isMaster) 0 else barsInsets.start(v),
			0,
			if (isTablet && isMaster) 0 else barsInsets.end(v),
			barsInsets.bottom,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onResume() {
		super.onResume()
		setTitle(if (titleId != 0) getString(titleId) else null)
		arguments?.getString(SettingsActivity.ARG_PREF_KEY)?.let {
			focusPreference(it)
			arguments?.remove(SettingsActivity.ARG_PREF_KEY)
		}
	}

	protected open fun setTitle(title: CharSequence?) {
		(activity as? SettingsActivity)?.setSectionTitle(title)
	}

	protected fun getWarningIcon(): Drawable? = context?.let { ctx ->
		ContextCompat.getDrawable(ctx, R.drawable.ic_alert_outline)?.also {
			it.setTint(ContextCompat.getColor(ctx, R.color.warning))
		}
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
