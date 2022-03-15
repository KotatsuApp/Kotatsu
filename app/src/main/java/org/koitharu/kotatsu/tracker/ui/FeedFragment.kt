package org.koitharu.kotatsu.tracker.ui

import android.os.Bundle
import android.view.*
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.FragmentFeedBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.tracker.ui.adapter.FeedAdapter
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.measureHeight
import org.koitharu.kotatsu.utils.progress.Progress

class FeedFragment : BaseFragment<FragmentFeedBinding>(), PaginationScrollListener.Callback,
	MangaListListener {

	private val viewModel by viewModel<FeedViewModel>()

	private var feedAdapter: FeedAdapter? = null
	private var updateStatusSnackbar: Snackbar? = null
	private var paddingVertical = 0
	private var paddingHorizontal = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentFeedBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		feedAdapter = FeedAdapter(get(), viewLifecycleOwner, this)
		with(binding.recyclerView) {
			adapter = feedAdapter
			setHasFixedSize(true)
			addOnScrollListener(PaginationScrollListener(4, this@FeedFragment))
			val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
			paddingHorizontal = spacing
			paddingVertical = resources.getDimensionPixelOffset(R.dimen.grid_spacing_outer)
			addItemDecoration(SpacingItemDecoration(spacing))
		}

		viewModel.content.observe(viewLifecycleOwner, this::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, this::onError)
		viewModel.onFeedCleared.observe(viewLifecycleOwner) {
			onFeedCleared()
		}
		TrackWorker.getProgressLiveData(view.context.applicationContext)
			.observe(viewLifecycleOwner, this::onUpdateProgressChanged)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_feed, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_update -> {
				TrackWorker.startNow(requireContext())
				Snackbar.make(
					binding.recyclerView,
					R.string.feed_will_update_soon,
					Snackbar.LENGTH_LONG,
				).show()
				true
			}
			R.id.action_clear_feed -> {
				MaterialAlertDialogBuilder(context ?: return false)
					.setTitle(R.string.clear_updates_feed)
					.setMessage(R.string.text_clear_updates_feed_prompt)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string.clear) { _, _ ->
						viewModel.clearFeed()
					}.show()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onDestroyView() {
		feedAdapter = null
		updateStatusSnackbar = null
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val headerHeight = (activity as? AppBarOwner)?.appBar?.measureHeight() ?: insets.top
		binding.recyclerView.updatePadding(
			top = headerHeight + paddingVertical,
			left = insets.left + paddingHorizontal,
			right = insets.right + paddingHorizontal,
			bottom = insets.bottom + paddingVertical,
		)
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onTagRemoveClick(tag: MangaTag) = Unit

	override fun onFilterClick() = Unit

	override fun onEmptyActionClick() = Unit

	private fun onListChanged(list: List<ListModel>) {
		feedAdapter?.items = list
	}

	private fun onFeedCleared() {
		Snackbar.make(
			binding.recyclerView,
			R.string.updates_feed_cleared,
			Snackbar.LENGTH_LONG
		).show()
	}

	private fun onError(e: Throwable) {
		Snackbar.make(
			binding.recyclerView,
			e.getDisplayMessage(resources),
			Snackbar.LENGTH_SHORT
		).show()
	}

	private fun onUpdateProgressChanged(progress: Progress?) {
		if (progress == null) {
			updateStatusSnackbar?.dismiss()
			updateStatusSnackbar = null
			return
		}
		val summaryText = getString(
			R.string.chapters_checking_progress,
			progress.value + 1,
			progress.total
		)
		updateStatusSnackbar?.setText(summaryText) ?: run {
			val snackbar =
				Snackbar.make(binding.recyclerView, summaryText, Snackbar.LENGTH_INDEFINITE)
			updateStatusSnackbar = snackbar
			snackbar.show()
		}
	}

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	companion object {

		fun newInstance() = FeedFragment()
	}
}