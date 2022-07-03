package org.koitharu.kotatsu.main.ui

import android.app.ActivityOptions
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.prefs.AppSection
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ActivityMainBinding
import org.koitharu.kotatsu.databinding.NavigationHeaderBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.favourites.ui.FavouritesContainerFragment
import org.koitharu.kotatsu.history.ui.HistoryListFragment
import org.koitharu.kotatsu.local.ui.LocalListFragment
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.search.ui.multi.MultiSearchActivity
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionFragment
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionViewModel
import org.koitharu.kotatsu.settings.AppUpdateChecker
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.newsources.NewSourcesDialogFragment
import org.koitharu.kotatsu.settings.onboard.OnboardDialogFragment
import org.koitharu.kotatsu.suggestions.ui.SuggestionsFragment
import org.koitharu.kotatsu.suggestions.ui.SuggestionsWorker
import org.koitharu.kotatsu.tracker.ui.FeedFragment
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.VoiceInputContract
import org.koitharu.kotatsu.utils.ext.*
import com.google.android.material.R as materialR

private const val TAG_PRIMARY = "primary"
private const val TAG_SEARCH = "search"

class MainActivity :
	BaseActivity<ActivityMainBinding>(),
	AppBarOwner,
	View.OnClickListener,
	View.OnFocusChangeListener,
	SearchSuggestionListener {

	private val viewModel by viewModel<MainViewModel>()
	private val searchSuggestionViewModel by viewModel<SearchSuggestionViewModel>()

	private lateinit var nav: NavigationBarView

	private lateinit var navHeaderBinding: NavigationHeaderBinding
	private var drawerToggle: ActionBarDrawerToggle? = null
	private var drawer: DrawerLayout? = null
	private val voiceInputLauncher = registerForActivityResult(VoiceInputContract(), VoiceInputCallback())

	override val appBar: AppBarLayout
		get() = binding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMainBinding.inflate(layoutInflater))
		navHeaderBinding = NavigationHeaderBinding.inflate(layoutInflater)
		nav = binding.bottomNav
		drawer = binding.root as? DrawerLayout
		drawerToggle = drawer?.let {
			ActionBarDrawerToggle(
				this,
				it,
				binding.toolbar,
				R.string.open_menu,
				R.string.close_menu
			).apply {
				setHomeAsUpIndicator(
					ContextCompat.getDrawable(this@MainActivity, materialR.drawable.abc_ic_ab_back_material)
				)
				setToolbarNavigationClickListener {
					binding.searchView.hideKeyboard()
					onBackPressed()
				}
				it.addDrawerListener(this)
				supportActionBar?.setDisplayHomeAsUpEnabled(true)
			}
		}

		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
			if (insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom > 0) {
				val elevation = binding.bottomNav.elevation
				window.setNavigationBarTransparentCompat(this@MainActivity, elevation)
			}
			insets
		}
		ViewCompat.requestApplyInsets(binding.root)

		with(binding.searchView) {
			onFocusChangeListener = this@MainActivity
			searchSuggestionListener = this@MainActivity
		}

		nav.setOnItemSelectedListener { item ->
			when (item.itemId) {
				R.id.nav_local_storage -> {
					viewModel.defaultSection = AppSection.HISTORY
					setPrimaryFragment(HistoryListFragment.newInstance())
				}
				R.id.nav_favourites -> {
					viewModel.defaultSection = AppSection.FAVOURITES
					setPrimaryFragment(FavouritesContainerFragment.newInstance())
				}
				R.id.nav_feed -> {
					viewModel.defaultSection = AppSection.FEED
					setPrimaryFragment(FeedFragment.newInstance())
				}
			}
			appBar.setExpanded(true)
			true
		}

		binding.fab.setOnClickListener(this@MainActivity)
		binding.searchView.isVoiceSearchEnabled = voiceInputLauncher.resolve(this, null) != null

		supportFragmentManager.findFragmentByTag(TAG_PRIMARY)?.let {
			if (it is HistoryListFragment) binding.fab.show() else binding.fab.hide()
		}
		if (savedInstanceState == null) {
			onFirstStart()
		}

		viewModel.onOpenReader.observe(this, this::onOpenReader)
		viewModel.onError.observe(this, this::onError)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.isResumeEnabled.observe(this, this::onResumeEnabledChanged)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		drawerToggle?.isDrawerIndicatorEnabled =
			drawer?.getDrawerLockMode(GravityCompat.START) == DrawerLayout.LOCK_MODE_UNLOCKED
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		drawerToggle?.syncState()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		drawerToggle?.onConfigurationChanged(newConfig)
	}

	override fun onBackPressed() {
		val fragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH)
		binding.searchView.clearFocus()
		when {
			fragment != null -> supportFragmentManager.commit {
				remove(fragment)
				setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
				runOnCommit { onSearchClosed() }
			}
			else -> binding.searchView.requestFocus()
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return drawerToggle?.onOptionsItemSelected(item) == true || when (item.itemId) {
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab -> viewModel.openLastReader()
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.toolbarCard.updateLayoutParams<MarginLayoutParams> {
			leftMargin = insets.left
			rightMargin = insets.right
		}
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onFocusChange(v: View?, hasFocus: Boolean) {
		val fragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH)
		if (v?.id == R.id.searchView && hasFocus) {
			if (fragment == null) {
				supportFragmentManager.commit {
					add(R.id.container, SearchSuggestionFragment.newInstance(), TAG_SEARCH)
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
		binding.searchView.query = query
		if (submit) {
			if (query.isNotEmpty()) {
				val source = searchSuggestionViewModel.getLocalSearchSource()
				if (source != null) {
					startActivity(SearchActivity.newIntent(this, source, query))
				} else {
					startActivity(MultiSearchActivity.newIntent(this, query))
				}
				searchSuggestionViewModel.saveQuery(query)
			}
		}
	}

	override fun onTagClick(tag: MangaTag) {
		startActivity(MangaListActivity.newIntent(this, setOf(tag)))
	}

	override fun onQueryChanged(query: String) {
		searchSuggestionViewModel.onQueryChanged(query)
	}

	override fun onVoiceSearchClick() {
		val options = binding.searchView.drawableEnd?.bounds?.let { bounds ->
			ActivityOptionsCompat.makeScaleUpAnimation(
				binding.searchView,
				bounds.centerX(),
				bounds.centerY(),
				bounds.width(),
				bounds.height(),
			)
		}
		voiceInputLauncher.tryLaunch(binding.searchView.hint?.toString(), options)
	}

	override fun onClearSearchHistory() {
		MaterialAlertDialogBuilder(this, materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
			.setTitle(R.string.clear_search_history)
			.setIcon(R.drawable.ic_clear_all)
			.setMessage(R.string.text_clear_search_history_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				searchSuggestionViewModel.clearSearchHistory()
			}.show()
	}

	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		adjustDrawerLock()
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		adjustDrawerLock()
	}

	private fun onOpenReader(manga: Manga) {
		val options = ActivityOptions.makeScaleUpAnimation(binding.fab, 0, 0, binding.fab.width, binding.fab.height)
		startActivity(ReaderActivity.newIntent(this, manga), options?.toBundle())
	}

	private fun onError(e: Throwable) {
		Snackbar.make(binding.container, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.fab.isEnabled = !isLoading
	}

	private fun onResumeEnabledChanged(isEnabled: Boolean) {
		adjustFabVisibility(isResumeEnabled = isEnabled)
	}

	private fun setPrimaryFragment(fragment: Fragment) {
		supportFragmentManager.beginTransaction()
			.replace(R.id.container, fragment, TAG_PRIMARY)
			.commit()
		adjustFabVisibility(topFragment = fragment)
	}

	private fun onSearchOpened() {
		TransitionManager.beginDelayedTransition(binding.appbar)
		drawerToggle?.isDrawerIndicatorEnabled = false
		binding.toolbarCard.updateLayoutParams<AppBarLayout.LayoutParams> {
			scrollFlags = SCROLL_FLAG_NO_SCROLL
		}
		binding.appbar.setBackgroundColor(getThemeColor(materialR.attr.colorSurfaceVariant))
		binding.appbar.updatePadding(left = 0, right = 0)
		adjustDrawerLock()
		adjustFabVisibility(isSearchOpened = true)
	}

	private fun onSearchClosed() {
		TransitionManager.beginDelayedTransition(binding.appbar)
		drawerToggle?.isDrawerIndicatorEnabled = true
		binding.toolbarCard.updateLayoutParams<AppBarLayout.LayoutParams> {
			scrollFlags = SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
		}
		binding.appbar.background = null
		val padding = resources.getDimensionPixelOffset(R.dimen.margin_normal)
		binding.appbar.updatePadding(left = padding, right = padding)
		adjustDrawerLock()
		adjustFabVisibility(isSearchOpened = false)
	}

	private fun onFirstStart() {
		lifecycleScope.launchWhenResumed {
			val isUpdateSupported = withContext(Dispatchers.Default) {
				TrackWorker.setup(applicationContext)
				SuggestionsWorker.setup(applicationContext)
				AppUpdateChecker.isUpdateSupported(this@MainActivity)
			}
			if (isUpdateSupported) {
				AppUpdateChecker(this@MainActivity).checkIfNeeded()
			}
			val settings = get<AppSettings>()
			when {
				!settings.isSourcesSelected -> OnboardDialogFragment.showWelcome(supportFragmentManager)
				settings.newSources.isNotEmpty() -> NewSourcesDialogFragment.show(supportFragmentManager)
			}
		}
	}

	private fun adjustFabVisibility(
		isResumeEnabled: Boolean = viewModel.isResumeEnabled.value == true,
		topFragment: Fragment? = supportFragmentManager.findFragmentByTag(TAG_PRIMARY),
		isSearchOpened: Boolean = supportFragmentManager.findFragmentByTag(TAG_SEARCH)?.isVisible == true,
	) {
		val fab = binding.fab
		if (isResumeEnabled && !isSearchOpened && topFragment is HistoryListFragment) {
			if (!fab.isVisible) {
				fab.show()
			}
		} else {
			if (fab.isVisible) {
				fab.hide()
			}
		}
	}

	private fun adjustDrawerLock() {
		val drawer = drawer ?: return
		val isLocked = actionModeDelegate.isActionModeStarted || (drawerToggle?.isDrawerIndicatorEnabled == false)
		drawer.setDrawerLockMode(
			if (isLocked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
		)
	}

	private inner class VoiceInputCallback : ActivityResultCallback<String?> {

		override fun onActivityResult(result: String?) {
			if (result != null) {
				binding.searchView.query = result
			}
		}
	}
}