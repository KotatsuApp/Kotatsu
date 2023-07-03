package org.koitharu.kotatsu.main.ui

import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.transition.MaterialFadeThrough
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.util.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.explore.ui.ExploreFragment
import org.koitharu.kotatsu.favourites.ui.container.FavouritesContainerFragment
import org.koitharu.kotatsu.history.ui.HistoryListFragment
import org.koitharu.kotatsu.tracker.ui.feed.FeedFragment
import java.util.LinkedList

private const val TAG_PRIMARY = "primary"

class MainNavigationDelegate(
	private val navBar: NavigationBarView,
	private val fragmentManager: FragmentManager,
) : OnBackPressedCallback(false), NavigationBarView.OnItemSelectedListener, NavigationBarView.OnItemReselectedListener {

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
		if (recyclerView.context.isAnimationsEnabled) {
			recyclerView.smoothScrollToPosition(0)
		} else {
			recyclerView.firstVisibleItemPosition = 0
		}
	}

	override fun handleOnBackPressed() {
		navBar.selectedItemId = R.id.nav_history
	}

	fun onCreate() {
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
		return setPrimaryFragment(
			when (itemId) {
				R.id.nav_history -> HistoryListFragment()
				R.id.nav_favourites -> FavouritesContainerFragment()
				R.id.nav_explore -> ExploreFragment()
				R.id.nav_feed -> FeedFragment()
				else -> return false
			},
		)
	}

	private fun getItemId(fragment: Fragment) = when (fragment) {
		is HistoryListFragment -> R.id.nav_history
		is FavouritesContainerFragment -> R.id.nav_favourites
		is ExploreFragment -> R.id.nav_explore
		is FeedFragment -> R.id.nav_feed
		else -> 0
	}

	private fun setPrimaryFragment(fragment: Fragment): Boolean {
		if (fragmentManager.isStateSaved) {
			return false
		}
		fragment.enterTransition = MaterialFadeThrough()
		fragmentManager.beginTransaction()
			.setReorderingAllowed(true)
			.replace(R.id.container, fragment, TAG_PRIMARY)
			.commit()
		onFragmentChanged(fragment, fromUser = true)
		return true
	}

	private fun onFragmentChanged(fragment: Fragment, fromUser: Boolean) {
		isEnabled = fragment !is HistoryListFragment
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
