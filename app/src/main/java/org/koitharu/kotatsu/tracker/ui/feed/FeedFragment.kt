package org.koitharu.kotatsu.tracker.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.TypedSpacingItemDecoration
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.databinding.FragmentFeedBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.tracker.ui.feed.adapter.FeedAdapter
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import org.koitharu.kotatsu.utils.ext.getThemeColor
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment :
	BaseFragment<FragmentFeedBinding>(),
	PaginationScrollListener.Callback,
	MangaListListener, SwipeRefreshLayout.OnRefreshListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<FeedViewModel>()

	private var feedAdapter: FeedAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentFeedBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		feedAdapter = FeedAdapter(coil, viewLifecycleOwner, this)
		with(binding.recyclerView) {
			adapter = feedAdapter
			setHasFixedSize(true)
			addOnScrollListener(PaginationScrollListener(4, this@FeedFragment))
			val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
			val decoration = TypedSpacingItemDecoration(
				FeedAdapter.ITEM_TYPE_FEED to 0,
				fallbackSpacing = spacing,
			)
			addItemDecoration(decoration)
		}
		with(binding.swipeRefreshLayout) {
			setProgressBackgroundColorSchemeColor(context.getThemeColor(com.google.android.material.R.attr.colorPrimary))
			setColorSchemeColors(context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
			setOnRefreshListener(this@FeedFragment)
		}
		addMenuProvider(
			FeedMenuProvider(
				binding.recyclerView,
				(activity as? BottomNavOwner)?.bottomNav,
				viewModel,
			),
		)

		viewModel.content.observe(viewLifecycleOwner, this::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, SnackbarErrorObserver(binding.recyclerView, this))
		viewModel.onFeedCleared.observe(viewLifecycleOwner) {
			onFeedCleared()
		}
		TrackWorker.getIsRunningLiveData(view.context.applicationContext)
			.observe(viewLifecycleOwner, this::onIsTrackerRunningChanged)
	}

	override fun onDestroyView() {
		feedAdapter = null
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	override fun onRefresh() {
		TrackWorker.startNow(context ?: return)
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	override fun onFilterClick(view: View?) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onListHeaderClick(item: ListHeader, view: View) = Unit

	private fun onListChanged(list: List<ListModel>) {
		feedAdapter?.items = list
	}

	private fun onFeedCleared() {
		val snackbar = Snackbar.make(
			binding.recyclerView,
			R.string.updates_feed_cleared,
			Snackbar.LENGTH_LONG,
		)
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		snackbar.show()
	}

	private fun onIsTrackerRunningChanged(isRunning: Boolean) {
		binding.swipeRefreshLayout.isRefreshing = isRunning
	}

	override fun onScrolledToEnd() {
		viewModel.requestMoreItems()
	}

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	override fun onReadClick(manga: Manga, view: View) = Unit

	override fun onTagClick(manga: Manga, tag: MangaTag, view: View) = Unit

	companion object {

		fun newInstance() = FeedFragment()
	}
}
