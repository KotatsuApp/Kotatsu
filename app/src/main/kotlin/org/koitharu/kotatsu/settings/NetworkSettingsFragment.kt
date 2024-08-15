package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.network.DoHProvider
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.setDefaultValueCompat
import org.koitharu.kotatsu.parsers.util.names
import java.net.Proxy
import javax.inject.Inject

@AndroidEntryPoint
class NetworkSettingsFragment :
	BasePreferenceFragment(R.string.network),
	SharedPreferences.OnSharedPreferenceChangeListener {

	@Inject
	lateinit var contentCache: MemoryContentCache

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_network)
		findPreference<ListPreference>(AppSettings.KEY_DOH)?.run {
			entryValues = DoHProvider.entries.names()
			setDefaultValueCompat(DoHProvider.NONE.name)
		}
		bindProxySummary()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_SSL_BYPASS -> {
				Snackbar.make(listView, R.string.settings_apply_restart_required, Snackbar.LENGTH_INDEFINITE).show()
			}

			AppSettings.KEY_PROXY_TYPE,
			AppSettings.KEY_PROXY_ADDRESS,
			AppSettings.KEY_PROXY_PORT -> {
				bindProxySummary()
			}
		}
	}

	private fun bindProxySummary() {
		findPreference<Preference>(AppSettings.KEY_PROXY)?.run {
			val type = settings.proxyType
			val address = settings.proxyAddress
			val port = settings.proxyPort
			summary = when {
				type == Proxy.Type.DIRECT -> context.getString(R.string.disabled)
				address.isNullOrEmpty() || port == 0 -> context.getString(R.string.invalid_proxy_configuration)
				else -> "$address:$port"
			}
		}
	}
}
