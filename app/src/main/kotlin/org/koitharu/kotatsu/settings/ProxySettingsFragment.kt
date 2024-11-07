package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.BaseHttpClient
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.settings.utils.EditTextBindListener
import org.koitharu.kotatsu.settings.utils.PasswordSummaryProvider
import org.koitharu.kotatsu.settings.utils.validation.DomainValidator
import org.koitharu.kotatsu.settings.utils.validation.PortNumberValidator
import java.net.Proxy
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@AndroidEntryPoint
class ProxySettingsFragment : BasePreferenceFragment(R.string.proxy),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private var testJob: Job? = null

	@Inject
	@BaseHttpClient
	lateinit var okHttpClient: OkHttpClient

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_proxy)
		findPreference<EditTextPreference>(AppSettings.KEY_PROXY_ADDRESS)?.setOnBindEditTextListener(
			EditTextBindListener(
				inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI,
				hint = null,
				validator = DomainValidator(),
			),
		)
		findPreference<EditTextPreference>(AppSettings.KEY_PROXY_PORT)?.setOnBindEditTextListener(
			EditTextBindListener(
				inputType = EditorInfo.TYPE_CLASS_NUMBER,
				hint = null,
				validator = PortNumberValidator(),
			),
		)
		findPreference<EditTextPreference>(AppSettings.KEY_PROXY_PASSWORD)?.let { pref ->
			pref.setOnBindEditTextListener(
				EditTextBindListener(
					inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
					hint = null,
					validator = null,
				),
			)
			pref.summaryProvider = PasswordSummaryProvider()
		}
		updateDependencies()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean = when (preference.key) {
		AppSettings.KEY_PROXY_TEST -> {
			testConnection()
			true
		}

		else -> super.onPreferenceTreeClick(preference)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_PROXY_TYPE -> updateDependencies()
		}
	}

	private fun updateDependencies() {
		val isProxyEnabled = settings.proxyType != Proxy.Type.DIRECT
		findPreference<Preference>(AppSettings.KEY_PROXY_ADDRESS)?.isEnabled = isProxyEnabled
		findPreference<Preference>(AppSettings.KEY_PROXY_PORT)?.isEnabled = isProxyEnabled
		findPreference<PreferenceCategory>(AppSettings.KEY_PROXY_AUTH)?.isEnabled = isProxyEnabled
		findPreference<Preference>(AppSettings.KEY_PROXY_LOGIN)?.isEnabled = isProxyEnabled
		findPreference<Preference>(AppSettings.KEY_PROXY_PASSWORD)?.isEnabled = isProxyEnabled
		findPreference<Preference>(AppSettings.KEY_PROXY_TEST)?.isEnabled = isProxyEnabled && testJob?.isActive != true
	}

	private fun testConnection() {
		testJob?.cancel()
		testJob = viewLifecycleScope.launch {
			val pref = findPreference<Preference>(AppSettings.KEY_PROXY_TEST)
			pref?.run {
				setSummary(R.string.loading_)
				isEnabled = false
			}
			try {
				withContext(Dispatchers.Default) {
					val request = Request.Builder()
						.get()
						.url("http://neverssl.com")
						.build()
					val response = okHttpClient.newCall(request).await()
					check(response.isSuccessful) { response.message }
				}
				showTestResult(null)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				showTestResult(e)
			} finally {
				pref?.run {
					isEnabled = true
					summary = null
				}
			}
		}
	}

	private fun showTestResult(error: Throwable?) {
		MaterialAlertDialogBuilder(requireContext())
			.setTitle(R.string.proxy)
			.setMessage(error?.getDisplayMessage(resources) ?: getString(R.string.connection_ok))
			.setPositiveButton(android.R.string.ok, null)
			.setCancelable(true)
			.show()
	}
}
