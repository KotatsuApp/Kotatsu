package org.koitharu.kotatsu.ui.list

import android.app.ActivityOptions
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import moxy.ktx.moxyPresenter
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSection
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.list.favourites.FavouritesContainerFragment
import org.koitharu.kotatsu.ui.list.feed.FeedFragment
import org.koitharu.kotatsu.ui.list.history.HistoryListFragment
import org.koitharu.kotatsu.ui.list.local.LocalListFragment
import org.koitharu.kotatsu.ui.list.remote.RemoteListFragment
import org.koitharu.kotatsu.ui.reader.ReaderActivity
import org.koitharu.kotatsu.ui.reader.ReaderState
import org.koitharu.kotatsu.ui.search.SearchHelper
import org.koitharu.kotatsu.ui.settings.AppUpdateChecker
import org.koitharu.kotatsu.ui.settings.SettingsActivity
import org.koitharu.kotatsu.ui.tracker.TrackWorker
import org.koitharu.kotatsu.ui.utils.protect.AppProtectHelper
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.resolveDp
import java.io.Closeable

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener,
	SharedPreferences.OnSharedPreferenceChangeListener, MainView, View.OnClickListener {

	private val presenter by moxyPresenter(factory = ::MainPresenter)

	private val settings by inject<AppSettings>()
	private lateinit var drawerToggle: ActionBarDrawerToggle
	private var closeable: Closeable? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		drawerToggle =
			ActionBarDrawerToggle(this, drawer, toolbar, R.string.open_menu, R.string.close_menu)
		drawer.addDrawerListener(drawerToggle)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		navigationView.setNavigationItemSelectedListener(this)
		settings.subscribe(this)

		with(fab) {
			imageTintList = ColorStateList.valueOf(Color.WHITE)
			isVisible = true
			setOnClickListener(this@MainActivity)
		}

		supportFragmentManager.findFragmentById(R.id.container)?.let {
			fab.isVisible = it is HistoryListFragment
		} ?: run {
			openDefaultSection()
		}
		if (AppProtectHelper.check(this)) {
			return
		}
		TrackWorker.setup(applicationContext)
		AppUpdateChecker(this).invoke()
	}

	override fun onDestroy() {
		closeable?.close()
		settings.unsubscribe(this)
		AppProtectHelper.lock()
		super.onDestroy()
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		drawerToggle.syncState()
		initSideMenu(MangaProviderFactory.getSources(includeHidden = false))
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		drawerToggle.onConfigurationChanged(newConfig)
	}

	override fun onBackPressed() {
		if (drawer.isDrawerOpen(navigationView)) {
			drawer.closeDrawer(navigationView)
		} else {
			super.onBackPressed()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.opt_main, menu)
		menu.findItem(R.id.action_search)?.let { menuItem ->
			closeable = SearchHelper.setupSearchView(menuItem)
		}
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return drawerToggle.onOptionsItemSelected(item) || when (item.itemId) {
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab -> presenter.openLastReader()
		}
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		if (item.groupId == R.id.group_remote_sources) {
			val source = MangaSource.values().getOrNull(item.itemId) ?: return false
			setPrimaryFragment(RemoteListFragment.newInstance(source))
		} else when (item.itemId) {
			R.id.nav_history -> {
				settings.defaultSection = AppSection.HISTORY
				setPrimaryFragment(HistoryListFragment.newInstance())
			}
			R.id.nav_favourites -> {
				settings.defaultSection = AppSection.FAVOURITES
				setPrimaryFragment(FavouritesContainerFragment.newInstance())
			}
			R.id.nav_local_storage -> {
				settings.defaultSection = AppSection.LOCAL
				setPrimaryFragment(LocalListFragment.newInstance())
			}
			R.id.nav_feed -> {
				settings.defaultSection = AppSection.FEED
				setPrimaryFragment(FeedFragment.newInstance())
			}
			R.id.nav_action_settings -> {
				startActivity(SettingsActivity.newIntent(this))
				return true
			}
			else -> return false
		}
		drawer.closeDrawers()
		return true
	}

	override fun onOpenReader(state: ReaderState) {
		val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			ActivityOptions.makeClipRevealAnimation(
				fab, 0, 0, fab.measuredWidth, fab.measuredHeight
			)
		} else {
			ActivityOptions.makeScaleUpAnimation(
				fab, 0, 0, fab.measuredWidth, fab.measuredHeight
			)
		}
		startActivity(ReaderActivity.newIntent(this, state), options?.toBundle())
	}

	override fun onError(e: Throwable) {
		Snackbar.make(container, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		fab.isEnabled = !isLoading
		if (isLoading) {
			fab.setImageDrawable(CircularProgressDrawable(this).also {
				it.setColorSchemeColors(Color.WHITE)
				it.strokeWidth = resources.resolveDp(2f)
				it.start()
			})
		} else {
			fab.setImageResource(R.drawable.ic_read_fill)
		}
	}

	private fun initSideMenu(remoteSources: List<MangaSource>) {
		val submenu = navigationView.menu.findItem(R.id.nav_remote_sources).subMenu
		submenu.removeGroup(R.id.group_remote_sources)
		remoteSources.forEachIndexed { index, source ->
			submenu.add(R.id.group_remote_sources, source.ordinal, index, source.title)
		}
		submenu.setGroupCheckable(R.id.group_remote_sources, true, true)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			getString(R.string.key_sources_hidden),
			getString(R.string.key_sources_order) -> {
				initSideMenu(MangaProviderFactory.getSources(includeHidden = false))
			}
		}
	}

	private fun openDefaultSection() {
		when (settings.defaultSection) {
			AppSection.LOCAL -> {
				navigationView.setCheckedItem(R.id.nav_local_storage)
				setPrimaryFragment(LocalListFragment.newInstance())
			}
			AppSection.FAVOURITES -> {
				navigationView.setCheckedItem(R.id.nav_favourites)
				setPrimaryFragment(FavouritesContainerFragment.newInstance())
			}
			AppSection.HISTORY -> {
				navigationView.setCheckedItem(R.id.nav_history)
				setPrimaryFragment(HistoryListFragment.newInstance())
			}
			AppSection.FEED -> {
				navigationView.setCheckedItem(R.id.nav_feed)
				setPrimaryFragment(FeedFragment.newInstance())
			}
		}
	}

	private fun setPrimaryFragment(fragment: Fragment) {
		supportFragmentManager.beginTransaction()
			.replace(R.id.container, fragment)
			.commit()
		fab.isVisible = fragment is HistoryListFragment
	}
}