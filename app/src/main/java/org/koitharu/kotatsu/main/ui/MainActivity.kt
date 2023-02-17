package org.koitharu.kotatsu.main.ui

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.util.SparseIntArray
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.util.size
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.widgets.SlidingBottomNavigationView
import org.koitharu.kotatsu.databinding.ActivityMainBinding
import org.koitharu.kotatsu.details.service.MangaPrefetchService
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.multi.MultiSearchActivity
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionFragment
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionViewModel
import org.koitharu.kotatsu.settings.newsources.NewSourcesDialogFragment
import org.koitharu.kotatsu.settings.onboard.OnboardDialogFragment
import org.koitharu.kotatsu.shelf.ui.ShelfFragment
import org.koitharu.kotatsu.suggestions.ui.SuggestionsWorker
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.VoiceInputContract
import org.koitharu.kotatsu.utils.ext.drawableEnd
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.hideKeyboard
import org.koitharu.kotatsu.utils.ext.resolve
import org.koitharu.kotatsu.utils.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.utils.ext.setNavigationBarTransparentCompat
import org.koitharu.kotatsu.utils.ext.tryLaunch
import com.google.android.material.R as materialR

private const val TAG_SEARCH = "search"

@AndroidEntryPoint
class MainActivity :
	BaseActivity<ActivityMainBinding>(),
	AppBarOwner,
	BottomNavOwner,
	View.OnClickListener,
	View.OnFocusChangeListener,
	SearchSuggestionListener,
	MainNavigationDelegate.OnFragmentChangedListener {

	private val viewModel by viewModels<MainViewModel>()
	private val searchSuggestionViewModel by viewModels<SearchSuggestionViewModel>()
	private val voiceInputLauncher = registerForActivityResult(VoiceInputContract(), VoiceInputCallback())
	private val closeSearchCallback = CloseSearchCallback()
	private lateinit var navigationDelegate: MainNavigationDelegate

	override val appBar: AppBarLayout
		get() = binding.appbar

	override val bottomNav: SlidingBottomNavigationView?
		get() = binding.bottomNav

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMainBinding.inflate(layoutInflater))

		if (bottomNav != null) {
			ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
				if (insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom > 0) {
					val elevation = bottomNav?.elevation ?: 0f
					window.setNavigationBarTransparentCompat(this@MainActivity, elevation)
				}
				insets
			}
			ViewCompat.requestApplyInsets(binding.root)
		}

		with(binding.searchView) {
			onFocusChangeListener = this@MainActivity
			searchSuggestionListener = this@MainActivity
		}
		window.statusBarColor = ContextCompat.getColor(this, R.color.dim_statusbar)

		binding.fab?.setOnClickListener(this)
		binding.navRail?.headerView?.setOnClickListener(this)
		binding.searchView.isVoiceSearchEnabled = voiceInputLauncher.resolve(this, null) != null

		navigationDelegate = MainNavigationDelegate(checkNotNull(bottomNav ?: binding.navRail), supportFragmentManager)
		navigationDelegate.addOnFragmentChangedListener(this)
		navigationDelegate.onCreate(savedInstanceState)

		onBackPressedDispatcher.addCallback(ExitCallback(this, binding.container))
		onBackPressedDispatcher.addCallback(navigationDelegate)
		onBackPressedDispatcher.addCallback(closeSearchCallback)

		if (savedInstanceState == null) {
			onFirstStart()
		}

		viewModel.onOpenReader.observe(this, this::onOpenReader)
		viewModel.onError.observe(this, this::onError)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.isResumeEnabled.observe(this, this::onResumeEnabledChanged)
		viewModel.counters.observe(this, ::onCountersChanged)
		viewModel.isFeedAvailable.observe(this, ::onFeedAvailabilityChanged)
		searchSuggestionViewModel.isIncognitoModeEnabled.observe(this, this::onIncognitoModeChanged)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		adjustSearchUI(isSearchOpened(), animate = false)
	}

	override fun onFragmentChanged(fragment: Fragment, fromUser: Boolean) {
		adjustFabVisibility(topFragment = fragment)
		if (fromUser) {
			binding.appbar.setExpanded(true)
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home && !isSearchOpened()) {
			binding.searchView.requestFocus()
			return true
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab -> viewModel.openLastReader()
			R.id.railFab -> viewModel.openLastReader()
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
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
					setReorderingAllowed(true)
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
				startActivity(MultiSearchActivity.newIntent(this, query))
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
		showNav(false)
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		adjustFabVisibility()
		showNav(true)
	}

	private fun onOpenReader(manga: Manga) {
		val fab = binding.fab ?: binding.navRail?.headerView
		val options = fab?.let {
			scaleUpActivityOptionsOf(it).toBundle()
		}
		startActivity(ReaderActivity.newIntent(this, manga), options)
	}

	private fun onError(e: Throwable) {
		Snackbar.make(binding.container, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	private fun onCountersChanged(counters: SparseIntArray) {
		repeat(counters.size) { i ->
			val id = counters.keyAt(i)
			val counter = counters.valueAt(i)
			navigationDelegate.setCounter(id, counter)
		}
	}

	private fun onFeedAvailabilityChanged(isFeedAvailable: Boolean) {
		navigationDelegate.setItemVisibility(R.id.nav_feed, isFeedAvailable)
	}

	private fun onIncognitoModeChanged(isIncognito: Boolean) {
		var options = binding.searchView.imeOptions
		options = if (isIncognito) {
			options or EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING
		} else {
			options and EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
		}
		binding.searchView.imeOptions = options
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.fab?.isEnabled = !isLoading
	}

	private fun onResumeEnabledChanged(isEnabled: Boolean) {
		adjustFabVisibility(isResumeEnabled = isEnabled)
	}

	private fun onSearchOpened() {
		adjustSearchUI(isOpened = true, animate = true)
		closeSearchCallback.isEnabled = true
	}

	private fun onSearchClosed() {
		binding.searchView.hideKeyboard()
		adjustSearchUI(isOpened = false, animate = true)
		closeSearchCallback.isEnabled = false
	}

	private fun showNav(visible: Boolean) {
		bottomNav?.run {
			if (visible) {
				show()
			} else {
				hide()
			}
		}
		binding.navRail?.isVisible = visible
	}

	private fun isSearchOpened(): Boolean {
		return supportFragmentManager.findFragmentByTag(TAG_SEARCH) != null
	}

	private fun onFirstStart() {
		lifecycleScope.launch(Dispatchers.Main) { // not a default `Main.immediate` dispatcher
			when {
				!settings.isSourcesSelected -> whenResumed {
					OnboardDialogFragment.showWelcome(supportFragmentManager)
				}

				settings.newSources.isNotEmpty() -> whenResumed {
					NewSourcesDialogFragment.show(supportFragmentManager)
				}
			}
			withContext(Dispatchers.Default) {
				TrackWorker.setup(applicationContext)
				SuggestionsWorker.setup(applicationContext)
			}
			whenResumed {
				MangaPrefetchService.prefetchLast(this@MainActivity)
				requestNotificationsPermission()
			}
		}
	}

	private fun adjustFabVisibility(
		isResumeEnabled: Boolean = viewModel.isResumeEnabled.value == true,
		topFragment: Fragment? = navigationDelegate.primaryFragment,
		isSearchOpened: Boolean = isSearchOpened(),
	) {
		val fab = binding.fab ?: return
		if (
			isResumeEnabled &&
			!actionModeDelegate.isActionModeStarted &&
			!isSearchOpened &&
			topFragment is ShelfFragment
		) {
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
			TransitionManager.beginDelayedTransition(binding.appbar)
		}
		val appBarScrollFlags = if (isOpened) {
			SCROLL_FLAG_NO_SCROLL
		} else {
			SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS or SCROLL_FLAG_SNAP
		}
		binding.toolbarCard.updateLayoutParams<AppBarLayout.LayoutParams> { scrollFlags = appBarScrollFlags }
		binding.insetsHolder.updateLayoutParams<AppBarLayout.LayoutParams> { scrollFlags = appBarScrollFlags }
		binding.toolbarCard.background = if (isOpened) {
			null
		} else {
			ContextCompat.getDrawable(this, R.drawable.toolbar_background)
		}
		val padding = if (isOpened) 0 else resources.getDimensionPixelOffset(R.dimen.margin_normal)
		binding.appbar.updatePadding(left = padding, right = padding)
		adjustFabVisibility(isSearchOpened = isOpened)
		supportActionBar?.setHomeAsUpIndicator(
			if (isOpened) materialR.drawable.abc_ic_ab_back_material else materialR.drawable.abc_ic_search_api_material,
		)
		showNav(!isOpened)
	}

	private fun requestNotificationsPermission() {
		if (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED
		) {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
		}
	}

	private inner class VoiceInputCallback : ActivityResultCallback<String?> {

		override fun onActivityResult(result: String?) {
			if (result != null) {
				binding.searchView.query = result
			}
		}
	}

	private inner class CloseSearchCallback : OnBackPressedCallback(false) {

		override fun handleOnBackPressed() {
			val fragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH)
			binding.searchView.clearFocus()
			if (fragment == null) {
				// this should not happen but who knows
				isEnabled = false
				return
			}
			supportFragmentManager.commit {
				setReorderingAllowed(true)
				remove(fragment)
				setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
				runOnCommit { onSearchClosed() }
			}
		}
	}
}
