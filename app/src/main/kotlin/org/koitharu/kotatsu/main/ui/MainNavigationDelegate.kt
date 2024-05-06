package org.koitharu.kotatsu.main.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.core.view.isEmpty
import androidx.core.view.iterator
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.ui.AllBookmarksFragment
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.NavItem
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.util.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.explore.ui.ExploreFragment
import org.koitharu.kotatsu.favourites.ui.container.FavouritesContainerFragment
import org.koitharu.kotatsu.history.ui.HistoryListFragment
import org.koitharu.kotatsu.local.ui.LocalListFragment
import org.koitharu.kotatsu.suggestions.ui.SuggestionsFragment
import org.koitharu.kotatsu.tracker.ui.feed.FeedFragment
import org.koitharu.kotatsu.tracker.ui.updates.UpdatesFragment
import java.util.LinkedList
import com.google.android.material.R as materialR

private const val TAG_PRIMARY = "primary"

class MainNavigationDelegate(
	private val navBar: NavigationBarView,
	private val fragmentManager: FragmentManager,
	private val settings: AppSettings,
) : OnBackPressedCallback(false),
	NavigationBarView.OnItemSelectedListener,
	NavigationBarView.OnItemReselectedListener {

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
		navBar.selectedItemId = firstItem()?.itemId ?: return
	}

	fun onCreate(lifecycleOwner: LifecycleOwner, savedInstanceState: Bundle?) {
		if (navBar.menu.isEmpty()) {
			createMenu(settings.mainNavItems, navBar.menu)
		}
		observeSettings(lifecycleOwner)
		val fragment = primaryFragment
		if (fragment != null) {
			onFragmentChanged(fragment, fromUser = false)
			val itemId = getItemId(fragment)
			if (navBar.selectedItemId != itemId) {
				navBar.selectedItemId = itemId
			}
		} else {
			val itemId = if (savedInstanceState == null) {
				firstItem()?.itemId ?: navBar.selectedItemId
			} else {
				navBar.selectedItemId
			}
			onNavigationItemSelected(itemId)
		}
	}

	fun setCounter(item: NavItem, counter: Int) {
		setCounter(item.id, counter)
	}

	private fun setCounter(@IdRes id: Int, counter: Int) {
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
				R.id.nav_history -> HistoryListFragment::class.java
				R.id.nav_favorites -> FavouritesContainerFragment::class.java
				R.id.nav_explore -> ExploreFragment::class.java
				R.id.nav_feed -> FeedFragment::class.java
				R.id.nav_local -> LocalListFragment::class.java
				R.id.nav_suggestions -> SuggestionsFragment::class.java
				R.id.nav_bookmarks -> AllBookmarksFragment::class.java
				R.id.nav_updated -> UpdatesFragment::class.java
				else -> return false
			},
		)
	}

	private fun getItemId(fragment: Fragment) = when (fragment) {
		is HistoryListFragment -> R.id.nav_history
		is FavouritesContainerFragment -> R.id.nav_favorites
		is ExploreFragment -> R.id.nav_explore
		is FeedFragment -> R.id.nav_feed
		is LocalListFragment -> R.id.nav_local
		is SuggestionsFragment -> R.id.nav_suggestions
		is AllBookmarksFragment -> R.id.nav_bookmarks
		is UpdatesFragment -> R.id.nav_updated
		else -> 0
	}

	private fun setPrimaryFragment(fragmentClass: Class<out Fragment>): Boolean {
		if (fragmentManager.isStateSaved || fragmentClass.isInstance(primaryFragment)) {
			return false
		}
		val fragment = instantiateFragment(fragmentClass)
		fragment.enterTransition = MaterialFadeThrough()
		fragmentManager.beginTransaction()
			.setReorderingAllowed(true)
			.replace(R.id.container, fragmentClass, null, TAG_PRIMARY)
			.runOnCommit { onFragmentChanged(fragment, fromUser = true) }
			.commit()
		return true
	}

	private fun onFragmentChanged(fragment: Fragment, fromUser: Boolean) {
		isEnabled = getItemId(fragment) != firstItem()?.itemId
		listeners.forEach { it.onFragmentChanged(fragment, fromUser) }
	}

	private fun createMenu(items: List<NavItem>, menu: Menu) {
		for (item in items) {
			menu.add(Menu.NONE, item.id, Menu.NONE, item.title)
				.setIcon(item.icon)
			if (menu.size >= navBar.maxItemCount) {
				break
			}
		}
	}

	private fun instantiateFragment(fragmentClass: Class<out Fragment>): Fragment {
		val classLoader = navBar.context.classLoader
		return fragmentManager.fragmentFactory.instantiate(classLoader, fragmentClass.name)
	}

	private fun observeSettings(lifecycleOwner: LifecycleOwner) {
		settings.observe()
			.filter { x ->
				x == AppSettings.KEY_TRACKER_ENABLED || x == AppSettings.KEY_SUGGESTIONS || x == AppSettings.KEY_NAV_LABELS
			}
			.onStart { emit("") }
			.flowOn(Dispatchers.IO)
			.onEach {
				setItemVisibility(R.id.nav_suggestions, settings.isSuggestionsEnabled)
				setItemVisibility(R.id.nav_feed, settings.isTrackerEnabled)
				setNavbarIsLabeled(settings.isNavLabelsVisible)
			}.launchIn(lifecycleOwner.lifecycleScope)
	}

	private fun firstItem(): MenuItem? {
		val menu = navBar.menu
		for (item in menu) {
			if (item.isVisible) return item
		}
		return null
	}

	private fun setNavbarIsLabeled(value: Boolean) {
		if (navBar is BottomNavigationView) {
			navBar.minimumHeight = navBar.resources.getDimensionPixelSize(
				if (value) {
					materialR.dimen.m3_bottom_nav_min_height
				} else {
					R.dimen.nav_bar_height_compact
				},
			)
		}
		navBar.labelVisibilityMode = if (value) {
			NavigationBarView.LABEL_VISIBILITY_LABELED
		} else {
			NavigationBarView.LABEL_VISIBILITY_UNLABELED
		}
	}

	interface OnFragmentChangedListener {

		fun onFragmentChanged(fragment: Fragment, fromUser: Boolean)
	}
}
