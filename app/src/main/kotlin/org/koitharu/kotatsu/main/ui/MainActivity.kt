package org.koitharu.kotatsu.main.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.SoftwareKeyboardControllerCompat
import androidx.core.view.children
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.NavItem
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.util.FadingAppbarMediator
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.ui.util.OptionsMenuBadgeHelper
import org.koitharu.kotatsu.core.ui.widgets.SlidingBottomNavigationView
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.databinding.ActivityMainBinding
import org.koitharu.kotatsu.details.service.MangaPrefetchService
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.favourites.ui.container.FavouritesContainerFragment
import org.koitharu.kotatsu.history.ui.HistoryListFragment
import org.koitharu.kotatsu.local.ui.LocalStorageCleanupWorker
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.main.ui.welcome.WelcomeSheet
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.multi.MultiSearchActivity
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionFragment
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionViewModel
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.about.AppUpdateActivity
import javax.inject.Inject
import com.google.android.material.R as materialR

private const val TAG_SEARCH = "search"

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), AppBarOwner, BottomNavOwner,
	View.OnClickListener,
	View.OnFocusChangeListener, SearchSuggestionListener,
	MainNavigationDelegate.OnFragmentChangedListener, View.OnLayoutChangeListener {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<MainViewModel>()
	private val searchSuggestionViewModel by viewModels<SearchSuggestionViewModel>()
	private val closeSearchCallback = CloseSearchCallback()
	private lateinit var navigationDelegate: MainNavigationDelegate
	private lateinit var appUpdateBadge: OptionsMenuBadgeHelper
	private lateinit var fadingAppbarMediator: FadingAppbarMediator

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override val bottomNav: SlidingBottomNavigationView?
		get() = viewBinding.bottomNav

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMainBinding.inflate(layoutInflater))

		with(viewBinding.searchView) {
			onFocusChangeListener = this@MainActivity
			searchSuggestionListener = this@MainActivity
		}

		viewBinding.fab?.setOnClickListener(this)
		viewBinding.navRail?.headerView?.setOnClickListener(this)
		fadingAppbarMediator = FadingAppbarMediator(viewBinding.appbar, viewBinding.toolbarCard)

		navigationDelegate = MainNavigationDelegate(
			navBar = checkNotNull(bottomNav ?: viewBinding.navRail),
			fragmentManager = supportFragmentManager,
			settings = settings,
		)
		navigationDelegate.addOnFragmentChangedListener(this)
		navigationDelegate.onCreate(this, savedInstanceState)

		appUpdateBadge = OptionsMenuBadgeHelper(viewBinding.toolbar, R.id.action_app_update)

		onBackPressedDispatcher.addCallback(ExitCallback(this, viewBinding.container))
		onBackPressedDispatcher.addCallback(navigationDelegate)
		onBackPressedDispatcher.addCallback(closeSearchCallback)

		if (savedInstanceState == null) {
			onFirstStart()
		}

		viewModel.onOpenReader.observeEvent(this, this::onOpenReader)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.container, null))
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.isResumeEnabled.observe(this, this::onResumeEnabledChanged)
		viewModel.feedCounter.observe(this, ::onFeedCounterChanged)
		viewModel.appUpdate.observe(this, MenuInvalidator(this))
		viewModel.onFirstStart.observeEvent(this) {
			WelcomeSheet.show(supportFragmentManager)
		}
		viewModel.isBottomNavPinned.observe(this, ::setNavbarPinned)
		searchSuggestionViewModel.isIncognitoModeEnabled.observe(this, this::onIncognitoModeChanged)
		viewBinding.bottomNav?.addOnLayoutChangeListener(this)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		adjustSearchUI(isSearchOpened(), animate = false)
	}

	override fun onFragmentChanged(fragment: Fragment, fromUser: Boolean) {
		adjustFabVisibility(topFragment = fragment)
		adjustAppbar(topFragment = fragment)
		if (fromUser) {
			actionModeDelegate.finishActionMode()
			closeSearchCallback.handleOnBackPressed()
			viewBinding.appbar.setExpanded(true)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.opt_main, menu)
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
		if (menu == null) {
			return false
		}
		menu.findItem(R.id.action_incognito)?.isChecked =
			searchSuggestionViewModel.isIncognitoModeEnabled.value
		val hasAppUpdate = viewModel.appUpdate.value != null
		menu.findItem(R.id.action_app_update)?.isVisible = hasAppUpdate
		appUpdateBadge.setBadgeVisible(hasAppUpdate)
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> if (isSearchOpened()) {
			closeSearchCallback.handleOnBackPressed()
			true
		} else {
			viewBinding.searchView.requestFocus()
			true
		}

		R.id.action_settings -> {
			startActivity(SettingsActivity.newIntent(this))
			true
		}

		R.id.action_incognito -> {
			viewModel.setIncognitoMode(!item.isChecked)
			true
		}

		R.id.action_app_update -> {
			startActivity(Intent(this, AppUpdateActivity::class.java))
			true
		}

		else -> super.onOptionsItemSelected(item)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab, R.id.railFab -> viewModel.openLastReader()
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onLayoutChange(
		v: View?,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int,
		oldLeft: Int,
		oldTop: Int,
		oldRight: Int,
		oldBottom: Int
	) {
		if (top != oldTop || bottom != oldBottom) {
			updateContainerBottomMargin()
		}
	}

	override fun onFocusChange(v: View?, hasFocus: Boolean) {
		val fragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH)
		if (v?.id == R.id.searchView && hasFocus) {
			if (fragment == null) {
				supportFragmentManager.commit {
					setReorderingAllowed(true)
					add(R.id.container, SearchSuggestionFragment.newInstance(), TAG_SEARCH)
					navigationDelegate.primaryFragment?.let {
						setMaxLifecycle(it, Lifecycle.State.STARTED)
					}
					setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
					runOnCommit { onSearchOpened() }
				}
			}
		}
	}

	override fun onMangaClick(manga: Manga) {
		startActivity(DetailsActivity.newIntent(this, manga))
	}

	override fun onQueryClick(query: String, submit: Boolean) {
		viewBinding.searchView.query = query
		if (submit && query.isNotEmpty()) {
			startActivity(MultiSearchActivity.newIntent(this, query))
			searchSuggestionViewModel.saveQuery(query)
			viewBinding.searchView.post {
				closeSearchCallback.handleOnBackPressed()
			}
		}
	}

	override fun onTagClick(tag: MangaTag) {
		startActivity(MangaListActivity.newIntent(this, setOf(tag)))
	}

	override fun onQueryChanged(query: String) {
		searchSuggestionViewModel.onQueryChanged(query)
	}

	override fun onSourceToggle(source: MangaSource, isEnabled: Boolean) {
		searchSuggestionViewModel.onSourceToggle(source, isEnabled)
	}

	override fun onSourceClick(source: MangaSource) {
		val intent = MangaListActivity.newIntent(this, source)
		startActivity(intent)
	}

	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		adjustFabVisibility()
		bottomNav?.hide()
		viewBinding.toolbarCard.isInvisible = true
		updateContainerBottomMargin()
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		adjustFabVisibility()
		bottomNav?.show()
		viewBinding.toolbarCard.isInvisible = false
		updateContainerBottomMargin()
	}

	private fun onOpenReader(manga: Manga) {
		val fab = viewBinding.fab ?: viewBinding.navRail?.headerView
		val options = fab?.let {
			scaleUpActivityOptionsOf(it)
		}
		startActivity(IntentBuilder(this).manga(manga).build(), options)
	}

	private fun onFeedCounterChanged(counter: Int) {
		navigationDelegate.setCounter(NavItem.FEED, counter)
	}

	private fun onIncognitoModeChanged(isIncognito: Boolean) {
		var options = viewBinding.searchView.imeOptions
		options = if (isIncognito) {
			options or EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING
		} else {
			options and EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
		}
		viewBinding.searchView.imeOptions = options
		invalidateMenu()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.fab?.isEnabled = !isLoading
	}

	private fun onResumeEnabledChanged(isEnabled: Boolean) {
		adjustFabVisibility(isResumeEnabled = isEnabled)
	}

	private fun onSearchOpened() {
		adjustSearchUI(isOpened = true, animate = true)
	}

	private fun onSearchClosed() {
		SoftwareKeyboardControllerCompat(viewBinding.searchView).hide()
		adjustSearchUI(isOpened = false, animate = true)
	}

	private fun isSearchOpened(): Boolean {
		return supportFragmentManager.findFragmentByTag(TAG_SEARCH) != null
	}

	private fun onFirstStart() {
		lifecycleScope.launch(Dispatchers.Main) { // not a default `Main.immediate` dispatcher
			withContext(Dispatchers.Default) {
				LocalStorageCleanupWorker.enqueue(applicationContext)
			}
			withResumed {
				MangaPrefetchService.prefetchLast(this@MainActivity)
				requestNotificationsPermission()
			}
		}
	}

	private fun adjustAppbar(topFragment: Fragment) {
		if (topFragment is FavouritesContainerFragment) {
			viewBinding.appbar.fitsSystemWindows = true
			fadingAppbarMediator.bind()
		} else {
			viewBinding.appbar.fitsSystemWindows = false
			fadingAppbarMediator.unbind()
		}
	}

	private fun adjustFabVisibility(
		isResumeEnabled: Boolean = viewModel.isResumeEnabled.value,
		topFragment: Fragment? = navigationDelegate.primaryFragment,
		isSearchOpened: Boolean = isSearchOpened(),
	) {
		val fab = viewBinding.fab ?: return
		if (isResumeEnabled && !actionModeDelegate.isActionModeStarted && !isSearchOpened && topFragment is HistoryListFragment) {
			if (!fab.isVisible) {
				fab.show()
			}
		} else {
			if (fab.isVisible) {
				fab.hide()
			}
		}
	}

	private fun adjustSearchUI(isOpened: Boolean, animate: Boolean) {
		if (animate) {
			TransitionManager.beginDelayedTransition(viewBinding.appbar)
		}
		val appBarScrollFlags = if (isOpened) {
			SCROLL_FLAG_NO_SCROLL
		} else {
			SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS or SCROLL_FLAG_SNAP
		}
		viewBinding.toolbarCard.updateLayoutParams<AppBarLayout.LayoutParams> {
			scrollFlags = appBarScrollFlags
		}
		viewBinding.insetsHolder.updateLayoutParams<AppBarLayout.LayoutParams> {
			scrollFlags = appBarScrollFlags
		}
		viewBinding.toolbarCard.background = if (isOpened) {
			null
		} else {
			ContextCompat.getDrawable(this, R.drawable.search_bar_background)
		}
		val padding = if (isOpened) 0 else resources.getDimensionPixelOffset(R.dimen.margin_normal)
		viewBinding.appbar.updatePadding(left = padding, right = padding)
		adjustFabVisibility(isSearchOpened = isOpened)
		supportActionBar?.apply {
			setHomeAsUpIndicator(
				if (isOpened) {
					materialR.drawable.abc_ic_ab_back_material
				} else {
					materialR.drawable.abc_ic_search_api_material
				},
			)
			setHomeActionContentDescription(
				if (isOpened) R.string.back else R.string.search,
			)
		}
		viewBinding.searchView.setHintCompat(
			if (isOpened) R.string.search_hint else R.string.search_manga,
		)
		bottomNav?.showOrHide(!isOpened)
		closeSearchCallback.isEnabled = isOpened
		updateContainerBottomMargin()
	}

	private fun requestNotificationsPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS,
			) != PERMISSION_GRANTED
		) {
			ActivityCompat.requestPermissions(
				this,
				arrayOf(Manifest.permission.POST_NOTIFICATIONS),
				1,
			)
		}
	}

	private fun setNavbarPinned(isPinned: Boolean) {
		val bottomNavBar = viewBinding.bottomNav
		bottomNavBar?.isPinned = isPinned
		for (view in viewBinding.appbar.children) {
			val lp = view.layoutParams as? AppBarLayout.LayoutParams ?: continue
			val scrollFlags = if (isPinned) {
				lp.scrollFlags and SCROLL_FLAG_SCROLL.inv()
			} else {
				lp.scrollFlags or SCROLL_FLAG_SCROLL
			}
			if (scrollFlags != lp.scrollFlags) {
				lp.scrollFlags = scrollFlags
				view.layoutParams = lp
			}
		}
		updateContainerBottomMargin()
	}

	private fun updateContainerBottomMargin() {
		val bottomNavBar = viewBinding.bottomNav ?: return
		val newMargin = if (bottomNavBar.isPinned && bottomNavBar.isShownOrShowing) bottomNavBar.height else 0
		with(viewBinding.container) {
			val params = layoutParams as MarginLayoutParams
			if (params.bottomMargin != newMargin) {
				params.bottomMargin = newMargin
				layoutParams = params
			}
		}
	}

	private inner class CloseSearchCallback : OnBackPressedCallback(false) {

		override fun handleOnBackPressed() {
			val fm = supportFragmentManager
			val fragment = fm.findFragmentByTag(TAG_SEARCH)
			viewBinding.searchView.clearFocus()
			if (fragment == null) {
				// this should not happen but who knows
				isEnabled = false
				return
			}
			fm.commit {
				setReorderingAllowed(true)
				remove(fragment)
				navigationDelegate.primaryFragment?.let {
					setMaxLifecycle(it, Lifecycle.State.RESUMED)
				}
				setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
				runOnCommit { onSearchClosed() }
			}
		}
	}
}
