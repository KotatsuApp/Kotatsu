package org.koitharu.kotatsu.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.activity_settings.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.BaseActivity

class SettingsActivity : BaseActivity(),
	PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val isTablet = container_side != null
		if (supportFragmentManager.findFragmentById(R.id.container) == null) {
			supportFragmentManager.commit {
				if (isTablet) {
					replace(R.id.container_side, SettingsHeadersFragment())
					replace(R.id.container, AppearanceSettingsFragment())
				} else {
					replace(R.id.container, SettingsHeadersFragment())
				}
			}
		}
	}

	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val fm = supportFragmentManager
		if (container_side != null && caller is SettingsHeadersFragment) {
			fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
		}
		val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment)
		fragment.arguments = pref.extras
		fragment.setTargetFragment(caller, 0)
		fm.commit {
			replace(R.id.container, fragment)
			if (container_side == null || caller !is SettingsHeadersFragment) {
				addToBackStack(null)
			}
		}
		return true
	}

	override fun onTitleChanged(title: CharSequence?, color: Int) {
		if (container_side == null) {
			super.onTitleChanged(title, color)
		} else {
			if (supportFragmentManager.backStackEntryCount == 0) {
				supportActionBar?.subtitle = null
			} else {
				supportActionBar?.subtitle = title
			}
		}
	}

	fun openMangaSourceSettings(mangaSource: MangaSource) {
		supportFragmentManager.commit {
			replace(R.id.container, SourceSettingsFragment.newInstance(mangaSource))
			addToBackStack(null)
		}
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
	}
}