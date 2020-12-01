package org.koitharu.kotatsu.tracker.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.databinding.FragmentFeedBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.tracker.ui.adapter.FeedAdapter
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.hasItems

class FeedFragment : BaseFragment<FragmentFeedBinding>(), PaginationScrollListener.Callback,
	OnListItemClickListener<Manga> {

	private val viewModel by viewModel<FeedViewModel>()

	private var feedAdapter: FeedAdapter? = null

	override fun getTitle() = context?.getString(R.string.updates)

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
			addItemDecoration(
				SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing))
			)
			setHasFixedSize(true)
			addOnScrollListener(PaginationScrollListener(4, this@FeedFragment))
		}

		viewModel.content.observe(viewLifecycleOwner, this::onListChanged)
		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.onError.observe(viewLifecycleOwner, this::onError)
		viewModel.isEmptyState.observe(viewLifecycleOwner, this::onEmptyStateChanged)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_feed, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_update -> {
			TrackWorker.startNow(requireContext())
			Snackbar.make(
				binding.recyclerView,
				R.string.feed_will_update_soon,
				Snackbar.LENGTH_LONG
			).show()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onDestroyView() {
		feedAdapter = null
		super.onDestroyView()
	}

	private fun onListChanged(list: List<Any>) {
		feedAdapter?.items = list
	}

	private fun onError(e: Throwable) {
		if (binding.recyclerView.hasItems) {
			Snackbar.make(
				binding.recyclerView,
				e.getDisplayMessage(resources),
				Snackbar.LENGTH_SHORT
			).show()
		} else {
			with(binding.textViewHolder) {
				text = e.getDisplayMessage(resources)
				setCompoundDrawablesRelativeWithIntrinsicBounds(
					0,
					R.drawable.ic_error_large,
					0,
					0
				)
				isVisible = true
			}
		}
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val hasItems = binding.recyclerView.hasItems
		binding.progressBar.isVisible = isLoading && !hasItems
	}

	private fun onEmptyStateChanged(isEmpty: Boolean) {
		if (isEmpty) {
			setUpEmptyListHolder()
		}
		binding.layoutHolder.isVisible = isEmpty
	}

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	private fun setUpEmptyListHolder() {
		with(binding.textViewHolder) {
			setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
			setText(R.string.text_feed_holder)
		}
	}

	companion object {

		fun newInstance() = FeedFragment()
	}
}