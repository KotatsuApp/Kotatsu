package org.koitharu.kotatsu.main.ui

import android.app.ActivityOptions
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSection
import org.koitharu.kotatsu.favourites.ui.FavouritesContainerFragment
import org.koitharu.kotatsu.history.ui.HistoryListFragment
import org.koitharu.kotatsu.local.ui.LocalListFragment
import org.koitharu.kotatsu.main.ui.protect.AppProtectHelper
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment
import org.koitharu.kotatsu.search.ui.SearchHelper
import org.koitharu.kotatsu.settings.AppUpdateChecker
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.tracker.ui.FeedFragment
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.resolveDp
import java.io.Closeable

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener,
	View.OnClickListener {

	private val viewModel by viewModel<MainViewModel>()

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

		with(fab) {
			imageTintList = ColorStateList.valueOf(Color.WHITE)
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
		AppUpdateChecker(this).launchIfNeeded()

		viewModel.onOpenReader.observe(this, this::onOpenReader)
		viewModel.onError.observe(this, this::onError)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.remoteSources.observe(this, this::updateSideMenu)
	}

	override fun onDestroy() {
		closeable?.close()
		AppProtectHelper.lock()
		super.onDestroy()
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		drawerToggle.syncState()
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
			R.id.fab -> viewModel.openLastReader()
		}
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		if (item.groupId == R.id.group_remote_sources) {
			val source = MangaSource.values().getOrNull(item.itemId) ?: return false
			setPrimaryFragment(RemoteListFragment.newInstance(source))
		} else when (item.itemId) {
			R.id.nav_history -> {
				viewModel.defaultSection = AppSection.HISTORY
				setPrimaryFragment(HistoryListFragment.newInstance())
			}
			R.id.nav_favourites -> {
				viewModel.defaultSection = AppSection.FAVOURITES
				setPrimaryFragment(FavouritesContainerFragment.newInstance())
			}
			R.id.nav_local_storage -> {
				viewModel.defaultSection = AppSection.LOCAL
				setPrimaryFragment(LocalListFragment.newInstance())
			}
			R.id.nav_feed -> {
				viewModel.defaultSection = AppSection.FEED
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

	private fun onOpenReader(state: ReaderState) {
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

	private fun onError(e: Throwable) {
		Snackbar.make(container, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
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

	private fun updateSideMenu(remoteSources: List<MangaSource>) {
		val submenu = navigationView.menu.findItem(R.id.nav_remote_sources).subMenu
		submenu.removeGroup(R.id.group_remote_sources)
		remoteSources.forEachIndexed { index, source ->
			submenu.add(R.id.group_remote_sources, source.ordinal, index, source.title)
		}
		submenu.setGroupCheckable(R.id.group_remote_sources, true, true)
	}

	private fun openDefaultSection() {
		when (viewModel.defaultSection) {
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