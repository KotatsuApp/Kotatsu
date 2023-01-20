package org.koitharu.kotatsu.main.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationBarView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.explore.ui.ExploreFragment
import org.koitharu.kotatsu.settings.tools.ToolsFragment
import org.koitharu.kotatsu.shelf.ui.ShelfFragment
import org.koitharu.kotatsu.tracker.ui.feed.FeedFragment
import java.util.LinkedList

private const val TAG_PRIMARY = "primary"

class MainNavigationDelegate(
	private val navBar: NavigationBarView,
	private val fragmentManager: FragmentManager,
) : NavigationBarView.OnItemSelectedListener, NavigationBarView.OnItemReselectedListener {

	private val listeners = LinkedList<OnFragmentChangedListener>()

	val primaryFragment: Fragment?
		get() = fragmentManager.findFragmentByTag(TAG_PRIMARY)

	init {
		navBar.setOnItemSelectedListener(this)
		navBar.setOnItemReselectedListener(this)
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		return onNavigationItemSelected(item.itemId)
	}

	override fun onNavigationItemReselected(item: MenuItem) {
		val fragment = fragmentManager.findFragmentByTag(TAG_PRIMARY)
		if (fragment == null || fragment !is RecyclerViewOwner || fragment.view == null) {
			return
		}
		val recyclerView = fragment.recyclerView
		recyclerView.smoothScrollToPosition(0)
	}

	fun onCreate(savedInstanceState: Bundle?) {
		primaryFragment?.let {
			onFragmentChanged(it, fromUser = false)
			val itemId = getItemId(it)
			if (navBar.selectedItemId != itemId) {
				navBar.selectedItemId = itemId
			}
		} ?: onNavigationItemSelected(navBar.selectedItemId)
	}

	fun setCounter(@IdRes id: Int, counter: Int) {
		if (counter == 0) {
			navBar.getBadge(id)?.isVisible = false
		} else {
			val badge = navBar.getOrCreateBadge(id)
			if (counter < 0) {
				badge.clearNumber()
			} else {
				badge.number = counter
			}
			badge.isVisible = true
		}
	}

	fun setItemVisibility(@IdRes itemId: Int, isVisible: Boolean) {
		val item = navBar.menu.findItem(itemId) ?: return
		item.isVisible = isVisible
		if (item.isChecked && !isVisible) {
			navBar.selectedItemId = firstItem()?.itemId ?: return
		}
	}

	fun addOnFragmentChangedListener(listener: OnFragmentChangedListener) {
		listeners.add(listener)
	}

	fun removeOnFragmentChangedListener(listener: OnFragmentChangedListener) {
		listeners.remove(listener)
	}

	private fun onNavigationItemSelected(@IdRes itemId: Int): Boolean {
		setPrimaryFragment(
			when (itemId) {
				R.id.nav_shelf -> ShelfFragment.newInstance()
				R.id.nav_explore -> ExploreFragment.newInstance()
				R.id.nav_feed -> FeedFragment.newInstance()
				R.id.nav_tools -> ToolsFragment.newInstance()
				else -> return false
			},
		)
		return true
	}

	private fun getItemId(fragment: Fragment) = when (fragment) {
		is ShelfFragment -> R.id.nav_shelf
		is ExploreFragment -> R.id.nav_explore
		is FeedFragment -> R.id.nav_feed
		is ToolsFragment -> R.id.nav_tools
		else -> 0
	}

	private fun setPrimaryFragment(fragment: Fragment) {
		fragmentManager.beginTransaction()
			.setReorderingAllowed(true)
			.replace(R.id.container, fragment, TAG_PRIMARY)
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			.commit()
		onFragmentChanged(fragment, fromUser = true)
	}

	private fun onFragmentChanged(fragment: Fragment, fromUser: Boolean) {
		listeners.forEach { it.onFragmentChanged(fragment, fromUser) }
	}

	private fun firstItem(): MenuItem? {
		val menu = navBar.menu
		for (item in menu) {
			if (item.isVisible) return item
		}
		return null
	}

	interface OnFragmentChangedListener {

		fun onFragmentChanged(fragment: Fragment, fromUser: Boolean)
	}
}
