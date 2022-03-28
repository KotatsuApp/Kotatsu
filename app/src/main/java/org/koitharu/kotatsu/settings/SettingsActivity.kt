package org.koitharu.kotatsu.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.AppBarLayout
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.databinding.ActivitySettingsBinding
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.isScrolledToTop

class SettingsActivity :
	BaseActivity<ActivitySettingsBinding>(),
	PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
	AppBarOwner,
	FragmentManager.OnBackStackChangedListener {

	override val appBar: AppBarLayout
		get() = binding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySettingsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		if (supportFragmentManager.findFragmentById(R.id.container) == null) {
			supportFragmentManager.commit {
				replace(R.id.container, MainSettingsFragment())
			}
		}
	}

	override fun onTitleChanged(title: CharSequence?, color: Int) {
		super.onTitleChanged(title, color)
		binding.collapsingToolbarLayout?.title = title
	}

	override fun onStart() {
		super.onStart()
		supportFragmentManager.addOnBackStackChangedListener(this)
	}

	override fun onStop() {
		supportFragmentManager.removeOnBackStackChangedListener(this)
		super.onStop()
	}

	override fun onBackStackChanged() {
		val fragment = supportFragmentManager.findFragmentById(R.id.container) as? RecyclerViewOwner ?: return
		val recyclerView = fragment.recyclerView
		recyclerView.post {
			binding.appbar.setExpanded(recyclerView.isScrolledToTop, false)
		}
	}

	override fun onPreferenceStartFragment(
		caller: PreferenceFragmentCompat,
		pref: Preference
	): Boolean {
		val fm = supportFragmentManager
		val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment ?: return false)
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
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			replace(R.id.container, fragment)
			setReorderingAllowed(true)
			addToBackStack(null)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	companion object {

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
	}
}