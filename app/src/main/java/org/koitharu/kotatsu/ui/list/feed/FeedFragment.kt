package org.koitharu.kotatsu.ui.list.feed

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_tracklogs.*
import moxy.MvpDelegate
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.common.list.PaginationScrollListener
import org.koitharu.kotatsu.ui.common.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.callOnScrollListeners
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.hasItems

class FeedFragment : BaseFragment(R.layout.fragment_tracklogs), FeedView,
	PaginationScrollListener.Callback, OnRecyclerItemClickListener<TrackingLogItem> {

	private val presenter by moxyPresenter(factory = ::FeedPresenter)

	private var adapter: FeedAdapter? = null

	override fun getTitle() = context?.getString(R.string.updates)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = FeedAdapter(this)
		recyclerView.adapter = adapter
		recyclerView.addItemDecoration(
			SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing))
		)
		recyclerView.setHasFixedSize(true)
		recyclerView.addOnScrollListener(PaginationScrollListener(4, this))
		if (savedInstanceState?.containsKey(MvpDelegate.MOXY_DELEGATE_TAGS_KEY) != true) {
			onRequestMoreItems(0)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_feed, menu)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onListChanged(list: List<TrackingLogItem>) {
		adapter?.replaceData(list)
		if (list.isEmpty()) {
			setUpEmptyListHolder()
			layout_holder.isVisible = true
		} else {
			layout_holder.isVisible = false
		}
		recyclerView.callOnScrollListeners()
	}

	override fun onListAppended(list: List<TrackingLogItem>) {
		adapter?.appendData(list)
		recyclerView.callOnScrollListeners()
	}

	override fun onListError(e: Throwable) {
		if (recyclerView.hasItems) {
			Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT)
				.show()
		} else {
			textView_holder.text = e.getDisplayMessage(resources)
			textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(
				0,
				R.drawable.ic_error_large,
				0,
				0
			)
			layout_holder.isVisible = true
		}
	}

	override fun onError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems
		if (isLoading) {
			layout_holder.isVisible = false
		}
	}

	override fun getItemsCount(): Int {
		return adapter?.itemCount ?: 0
	}

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(offset)
	}

	override fun onItemClick(item: TrackingLogItem, position: Int, view: View) {
		startActivity(MangaDetailsActivity.newIntent(context ?: return, item.manga))
	}

	private fun setUpEmptyListHolder() {
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
		textView_holder.setText(R.string.text_feed_holder)
	}

	companion object {

		fun newInstance() = FeedFragment()
	}
}