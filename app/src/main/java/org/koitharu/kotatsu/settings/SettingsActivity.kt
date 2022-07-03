package org.koitharu.kotatsu.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.databinding.ActivitySettingsBinding
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.scrobbling.shikimori.ui.ShikimoriSettingsFragment
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
		menuInflater.inflate(R.menu.opt_settings, menu)
		return super.onCreateOptionsMenu(menu)
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
			Intent.ACTION_VIEW -> handleUri(intent.data) ?: return
			ACTION_READER -> ReaderSettingsFragment()
			ACTION_SUGGESTIONS -> SuggestionsSettingsFragment()
			ACTION_SHIKIMORI -> ShikimoriSettingsFragment()
			ACTION_TRACKER -> TrackerSettingsFragment()
			ACTION_SOURCE -> SourceSettingsFragment.newInstance(
				intent.getSerializableExtra(EXTRA_SOURCE) as? MangaSource ?: MangaSource.LOCAL
			)
			else -> SettingsHeadersFragment()
		}
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, fragment)
		}
	}

	private fun handleUri(uri: Uri?): Fragment? {
		when (uri?.host) {
			HOST_SHIKIMORI_AUTH ->
				return ShikimoriSettingsFragment.newInstance(authCode = uri.getQueryParameter("code"))
		}
		finishAfterTransition()
		return null
	}

	companion object {

		private const val ACTION_READER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"
		private const val ACTION_SUGGESTIONS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SUGGESTIONS"
		private const val ACTION_TRACKER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_TRACKER"
		private const val ACTION_SOURCE = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCE_SETTINGS"
		private const val ACTION_SHIKIMORI = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SHIKIMORI_SETTINGS"
		private const val EXTRA_SOURCE = "source"

		private const val HOST_SHIKIMORI_AUTH = "shikimori-auth"

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)

		fun newReaderSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_READER)

		fun newShikimoriSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SHIKIMORI)

		fun newSuggestionsSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SUGGESTIONS)

		fun newTrackerSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_TRACKER)

		fun newSourceSettingsIntent(context: Context, source: MangaSource) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCE)
				.putExtra(EXTRA_SOURCE, source)
	}
}