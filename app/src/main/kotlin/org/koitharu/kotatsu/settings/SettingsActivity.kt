package org.koitharu.kotatsu.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
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
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.core.util.ext.isScrolledToTop
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ActivitySettingsBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.about.AboutSettingsFragment
import org.koitharu.kotatsu.settings.about.AppUpdateDialog
import org.koitharu.kotatsu.settings.sources.SourceSettingsFragment
import org.koitharu.kotatsu.settings.sources.SourcesSettingsFragment
import org.koitharu.kotatsu.settings.sources.manage.SourcesManageFragment
import org.koitharu.kotatsu.settings.tracker.TrackerSettingsFragment
import org.koitharu.kotatsu.settings.userdata.UserDataSettingsFragment

@AndroidEntryPoint
class SettingsActivity :
	BaseActivity<ActivitySettingsBinding>(),
	PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
	AppBarOwner,
	FragmentManager.OnBackStackChangedListener {

	val appUpdateDialog = AppUpdateDialog(this)

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySettingsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val isMasterDetails = viewBinding.containerMaster != null
		val fm = supportFragmentManager
		val currentFragment = fm.findFragmentById(R.id.container)
		if (currentFragment == null || (isMasterDetails && currentFragment is RootSettingsFragment)) {
			openDefaultFragment()
		}
		if (isMasterDetails && fm.findFragmentById(R.id.container_master) == null) {
			supportFragmentManager.commit {
				setReorderingAllowed(true)
				replace(R.id.container_master, RootSettingsFragment())
			}
		}
	}

	override fun onTitleChanged(title: CharSequence?, color: Int) {
		super.onTitleChanged(title, color)
		viewBinding.collapsingToolbarLayout?.title = title
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
			viewBinding.appbar.setExpanded(recyclerView.isScrolledToTop, false)
		}
	}

	override fun onPreferenceStartFragment(
		caller: PreferenceFragmentCompat,
		pref: Preference,
	): Boolean {
		val fm = supportFragmentManager
		val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment ?: return false)
		fragment.arguments = pref.extras
		openFragment(fragment, isFromRoot = caller is RootSettingsFragment)
		return true
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.cardDetails?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = marginStart + insets.bottom
		}
	}

	fun setSectionTitle(title: CharSequence?) {
		viewBinding.textViewHeader?.apply {
			textAndVisible = title
		} ?: setTitle(title ?: getString(R.string.settings))
	}

	fun openFragment(fragment: Fragment, isFromRoot: Boolean) {
		val hasFragment = supportFragmentManager.findFragmentById(R.id.container) != null
		val isMasterDetail = viewBinding.containerMaster != null
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, fragment)
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
			if (!isMasterDetail || (hasFragment && !isFromRoot)) {
				addToBackStack(null)
			}
		}
	}

	private fun openDefaultFragment() {
		val hasMaster = viewBinding.containerMaster != null
		val fragment = when (intent?.action) {
			ACTION_READER -> ReaderSettingsFragment()
			ACTION_SUGGESTIONS -> SuggestionsSettingsFragment()
			ACTION_HISTORY -> UserDataSettingsFragment()
			ACTION_TRACKER -> TrackerSettingsFragment()
			ACTION_SOURCES -> SourcesSettingsFragment()
			ACTION_MANAGE_DOWNLOADS -> DownloadsSettingsFragment()
			ACTION_SOURCE -> SourceSettingsFragment.newInstance(
				intent.getSerializableExtraCompat(EXTRA_SOURCE) ?: MangaSource.LOCAL,
			)

			ACTION_MANAGE_SOURCES -> SourcesManageFragment()
			Intent.ACTION_VIEW -> {
				when (intent.data?.host) {
					HOST_ABOUT -> AboutSettingsFragment()
					HOST_SYNC_SETTINGS -> SyncSettingsFragment()
					else -> null
				}
			}

			else -> null
		} ?: if (hasMaster) AppearanceSettingsFragment() else RootSettingsFragment()
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
		private const val ACTION_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES"
		private const val ACTION_MANAGE_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES_LIST"
		private const val ACTION_MANAGE_DOWNLOADS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_DOWNLOADS"
		private const val EXTRA_SOURCE = "source"
		private const val HOST_ABOUT = "about"
		private const val HOST_SYNC_SETTINGS = "sync-settings"

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

		fun newSourcesSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCES)

		fun newManageSourcesIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_SOURCES)

		fun newDownloadsSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_DOWNLOADS)

		fun newSourceSettingsIntent(context: Context, source: MangaSource) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCE)
				.putExtra(EXTRA_SOURCE, source)
	}
}
