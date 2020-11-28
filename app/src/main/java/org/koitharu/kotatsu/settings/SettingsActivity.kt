package org.koitharu.kotatsu.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.MangaSource

class SettingsActivity : BaseActivity(),
	PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		if (supportFragmentManager.findFragmentById(R.id.container) == null) {
			supportFragmentManager.commit {
				replace(R.id.container, MainSettingsFragment())
			}
		}
	}

	@Suppress("DEPRECATION")
	override fun onPreferenceStartFragment(
		caller: PreferenceFragmentCompat,
		pref: Preference
	): Boolean {
		val fm = supportFragmentManager
		val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment)
		fragment.arguments = pref.extras
		fragment.setTargetFragment(caller, 0)
		openFragment(fragment)
		return true
	}

	fun openMangaSourceSettings(mangaSource: MangaSource) {
		openFragment(SourceSettingsFragment.newInstance(mangaSource))
	}

	fun openNotificationSettingsLegacy() {
		openFragment(NotificationSettingsLegacyFragment())
	}

	private fun openFragment(fragment: Fragment) {
		supportFragmentManager.commit {
			replace(R.id.container, fragment)
			setReorderingAllowed(true)
			addToBackStack(null)
		}
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
	}
}