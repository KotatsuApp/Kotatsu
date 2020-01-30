package org.koitharu.kotatsu.ui.main

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import kotlinx.android.synthetic.main.activity_main.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.main.list.MangaListFragment

class MainActivity : BaseActivity() {

	private lateinit var drawerToggle: ActionBarDrawerToggle

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		drawerToggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.open_menu, R.string.close_menu)
		drawer.addDrawerListener(drawerToggle)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setHomeButtonEnabled(true)

		if (!supportFragmentManager.isStateSaved) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.container, MangaListFragment.newInstance(MangaSource.READMANGA_RU))
				.commit()
		}
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		drawerToggle.syncState()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		drawerToggle.onConfigurationChanged(newConfig)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
	}
}