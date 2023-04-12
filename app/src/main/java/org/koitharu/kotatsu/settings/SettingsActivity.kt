package org.koitharu.kotatsu.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.databinding.ActivitySettingsBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.about.AboutSettingsFragment
import org.koitharu.kotatsu.settings.sources.SourcesListFragment
import org.koitharu.kotatsu.settings.tracker.TrackerSettingsFragment
import org.koitharu.kotatsu.utils.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.utils.ext.isScrolledToTop

@AndroidEntryPoint
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
			openDefaultFragment()
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

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.opt_settings, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.action_leaks -> {
			val intent = Intent()
			intent.component = ComponentName(this, "leakcanary.internal.activity.LeakActivity")
			intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			startActivity(intent)
			true
		}

		else -> super.onOptionsItemSelected(item)
	}

	override fun onBackStackChanged() {
		val fragment = supportFragmentManager.findFragmentById(R.id.container) as? RecyclerViewOwner ?: return
		val recyclerView = fragment.recyclerView
		recyclerView.post {
			binding.appbar.setExpanded(recyclerView.isScrolledToTop, false)
		}
	}

	@Suppress("DEPRECATION")
	override fun onPreferenceStartFragment(
		caller: PreferenceFragmentCompat,
		pref: Preference,
	): Boolean {
		val fm = supportFragmentManager
		val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment ?: return false)
		fragment.arguments = pref.extras
		fragment.setTargetFragment(caller, 0)
		openFragment(fragment)
		return true
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.appbar.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.container.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	fun openFragment(fragment: Fragment) {
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, fragment)
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			addToBackStack(null)
		}
	}

	private fun openDefaultFragment() {
		val fragment = when (intent?.action) {
			ACTION_READER -> ReaderSettingsFragment()
			ACTION_SUGGESTIONS -> SuggestionsSettingsFragment()
			ACTION_HISTORY -> HistorySettingsFragment()
			ACTION_TRACKER -> TrackerSettingsFragment()
			ACTION_SOURCE -> SourceSettingsFragment.newInstance(
				intent.getSerializableExtraCompat(EXTRA_SOURCE) as? MangaSource ?: MangaSource.LOCAL,
			)

			ACTION_MANAGE_SOURCES -> SourcesListFragment()
			Intent.ACTION_VIEW -> {
				when (intent.data?.host) {
					HOST_ABOUT -> AboutSettingsFragment()
					else -> SettingsHeadersFragment()
				}
			}

			else -> SettingsHeadersFragment()
		}
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, fragment)
		}
	}

	companion object {

		private const val ACTION_READER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"
		private const val ACTION_SUGGESTIONS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SUGGESTIONS"
		private const val ACTION_TRACKER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_TRACKER"
		private const val ACTION_HISTORY = "${BuildConfig.APPLICATION_ID}.action.MANAGE_HISTORY"
		private const val ACTION_SOURCE = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCE_SETTINGS"
		private const val ACTION_MANAGE_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES_LIST"
		private const val EXTRA_SOURCE = "source"
		private const val HOST_ABOUT = "about"

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)

		fun newReaderSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_READER)

		fun newSuggestionsSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SUGGESTIONS)

		fun newTrackerSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_TRACKER)

		fun newHistorySettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_HISTORY)

		fun newManageSourcesIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_SOURCES)

		fun newSourceSettingsIntent(context: Context, source: MangaSource) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCE)
				.putExtra(EXTRA_SOURCE, source)
	}
}
