package org.koitharu.kotatsu.ui.main

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import moxy.ktx.moxyPresenter
import org.koin.core.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.main.list.favourites.FavouritesListFragment
import org.koitharu.kotatsu.ui.main.list.history.HistoryListFragment
import org.koitharu.kotatsu.ui.main.list.local.LocalListFragment
import org.koitharu.kotatsu.ui.main.list.remote.RemoteListFragment
import org.koitharu.kotatsu.ui.reader.ReaderActivity
import org.koitharu.kotatsu.ui.reader.ReaderState
import org.koitharu.kotatsu.ui.settings.SettingsActivity
import org.koitharu.kotatsu.ui.settings.AppUpdateService
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.resolveDp

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener,
	SharedPreferences.OnSharedPreferenceChangeListener, MainView {

	private val presenter by moxyPresenter(factory = ::MainPresenter)

	private val settings by inject<AppSettings>()
	private lateinit var drawerToggle: ActionBarDrawerToggle

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		drawerToggle =
			ActionBarDrawerToggle(this, drawer, toolbar, R.string.open_menu, R.string.close_menu)
		drawer.addDrawerListener(drawerToggle)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setHomeButtonEnabled(true)

		navigationView.setNavigationItemSelectedListener(this)
		settings.subscribe(this)

		fab.imageTintList = ColorStateList.valueOf(Color.WHITE)
		fab.isVisible = true
		fab.setOnClickListener {
			presenter.openLastReader()
		}

		supportFragmentManager.findFragmentById(R.id.container)?.let {
			fab.isVisible = it is HistoryListFragment
		} ?: run {
			navigationView.setCheckedItem(R.id.nav_history)
			setPrimaryFragment(HistoryListFragment.newInstance())
		}
		drawer.postDelayed(4000) {
			AppUpdateService.startIfRequired(applicationContext)
		}
	}

	override fun onDestroy() {
		settings.unsubscribe(this)
		super.onDestroy()
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		drawerToggle.syncState()
		initSideMenu(MangaProviderFactory.sources)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		drawerToggle.onConfigurationChanged(newConfig)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_main, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return drawerToggle.onOptionsItemSelected(item) || when (item.itemId) {
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		if (item.groupId == R.id.group_remote_sources) {
			val source = MangaSource.values().getOrNull(item.itemId) ?: return false
			setPrimaryFragment(RemoteListFragment.newInstance(source))
		} else when (item.itemId) {
			R.id.nav_history -> setPrimaryFragment(HistoryListFragment.newInstance())
			R.id.nav_favourites -> setPrimaryFragment(FavouritesListFragment.newInstance())
			R.id.nav_local_storage -> setPrimaryFragment(LocalListFragment.newInstance())
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
		startActivity(ReaderActivity.newIntent(this, state))
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
			getString(R.string.key_sources_order) -> initSideMenu(MangaProviderFactory.sources)
		}
	}

	private fun setPrimaryFragment(fragment: Fragment) {
		supportFragmentManager.beginTransaction()
			.replace(R.id.container, fragment)
			.commit()
		fab.isVisible = fragment is HistoryListFragment
	}
}