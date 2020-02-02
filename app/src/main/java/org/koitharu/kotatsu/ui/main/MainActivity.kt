package org.koitharu.kotatsu.ui.main

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.main.list.history.HistoryListFragment
import org.koitharu.kotatsu.ui.main.list.remote.RemoteListFragment

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

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

		if (!supportFragmentManager.isStateSaved) {
			setPrimaryFragment(RemoteListFragment.newInstance(MangaSource.READMANGA_RU))
		}
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		drawerToggle.syncState()
		initSideMenu(MangaSource.values().asList())
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		drawerToggle.onConfigurationChanged(newConfig)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		if (item.groupId == R.id.group_remote_sources) {
			val source = MangaSource.values().getOrNull(item.itemId) ?: return false
			setPrimaryFragment(RemoteListFragment.newInstance(source))
		} else when (item.itemId) {
			R.id.nav_history -> setPrimaryFragment(HistoryListFragment.newInstance())
			R.id.nav_favourites -> Unit
			R.id.nav_local_storage -> Unit
			else -> return false
		}
		drawer.closeDrawers()
		return true
	}

	private fun initSideMenu(remoteSources: List<MangaSource>) {
		val submenu = navigationView.menu.findItem(R.id.nav_remote_sources).subMenu
		submenu.removeGroup(R.id.group_remote_sources)
		remoteSources.forEachIndexed { index, source ->
			submenu.add(R.id.group_remote_sources, source.ordinal, index, source.title)
		}
		submenu.setGroupCheckable(R.id.group_remote_sources, true, true)
	}

	private fun setPrimaryFragment(fragment: Fragment) {
		supportFragmentManager.beginTransaction()
			.replace(R.id.container, fragment)
			.commit()
	}
}