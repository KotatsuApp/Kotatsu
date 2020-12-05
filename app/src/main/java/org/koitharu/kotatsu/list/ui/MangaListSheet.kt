package org.koitharu.kotatsu.list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.databinding.SheetListBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListSheet : BaseBottomSheet<SheetListBinding>(),
	PaginationScrollListener.Callback, OnListItemClickListener<Manga>,
	Toolbar.OnMenuItemClickListener {

	private var listAdapter: MangaListAdapter? = null
	private var paginationListener: PaginationScrollListener? = null
	private val spanResolver = MangaListSpanResolver()
	private val spanSizeLookup = SpanSizeLookup()
	open val isSwipeRefreshEnabled = true

	protected abstract val viewModel: MangaListViewModel

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetListBinding {
		return SheetListBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		listAdapter = MangaListAdapter(get(), viewLifecycleOwner, this) {
			viewModel.onRetry()
		}
		paginationListener = PaginationScrollListener(4, this)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = listAdapter
			addOnScrollListener(paginationListener!!)
		}
		with(binding.toolbar) {
			inflateMenu(R.menu.opt_list_sheet)
			setOnMenuItemClickListener(this@MangaListSheet)
			setNavigationOnClickListener {
				dismiss()
			}
		}
		if (dialog !is BottomSheetDialog) {
			binding.toolbar.isVisible = true
			binding.textViewTitle.isVisible = false
			binding.appbar.elevation = resources.getDimension(R.dimen.elevation_large)
		}

		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.listMode.observe(viewLifecycleOwner, ::onListModeChanged)
		viewModel.gridScale.observe(viewLifecycleOwner, ::onGridScaleChanged)
	}

	override fun onDestroyView() {
		listAdapter = null
		paginationListener = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	protected fun setTitle(title: CharSequence) {
		binding.toolbar.title = title
		binding.textViewTitle.text = title
	}

	protected fun setSubtitle(subtitle: CharSequence) {
		binding.toolbar.subtitle = subtitle
	}

	override fun onCreateDialog(savedInstanceState: Bundle?) =
		super.onCreateDialog(savedInstanceState).also {
			val behavior = (it as? BottomSheetDialog)?.behavior ?: return@also
			behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
				private val elevation = resources.getDimension(R.dimen.elevation_large)

				override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_EXPANDED) {
						binding.toolbar.isVisible = true
						binding.textViewTitle.isVisible = false
						binding.appbar.elevation = elevation
					} else {
						binding.toolbar.isVisible = false
						binding.textViewTitle.isVisible = true
						binding.appbar.elevation = 0f
					}
				}
			})

		}

	override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
		R.id.action_list_mode -> {
			ListModeSelectDialog.show(childFragmentManager)
			true
		}
		else -> false
	}

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	private fun onListChanged(list: List<ListModel>) {
		spanSizeLookup.invalidateCache()
		listAdapter?.items = list
	}

	private fun onError(e: Throwable) {
		if (e is CloudFlareProtectedException) {
			CloudFlareDialog.newInstance(e.url).show(childFragmentManager, CloudFlareDialog.TAG)
		} else {
			Snackbar.make(
				binding.recyclerView,
				e.getDisplayMessage(resources),
				Snackbar.LENGTH_SHORT
			).show()
		}
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible =
			isLoading && !binding.recyclerView.hasItems
	}

	private fun onGridScaleChanged(scale: Float) {
		spanSizeLookup.invalidateCache()
		spanResolver.setGridSize(scale, binding.recyclerView)
	}

	private fun onListModeChanged(mode: ListMode) {
		spanSizeLookup.invalidateCache()
		with(binding.recyclerView) {
			clearItemDecorations()
			removeOnLayoutChangeListener(spanResolver)
			when (mode) {
				ListMode.LIST -> {
					layoutManager = LinearLayoutManager(context)
					addItemDecoration(
						DividerItemDecoration(
							context,
							RecyclerView.VERTICAL
						)
					)
				}
				ListMode.DETAILED_LIST -> {
					layoutManager = LinearLayoutManager(context)
					addItemDecoration(
						SpacingItemDecoration(
							resources.getDimensionPixelOffset(R.dimen.grid_spacing)
						)
					)
				}
				ListMode.GRID -> {
					layoutManager = GridLayoutManager(context, spanResolver.spanCount).also {
						it.spanSizeLookup = spanSizeLookup
					}
					addItemDecoration(
						SpacingItemDecoration(
							resources.getDimensionPixelOffset(R.dimen.grid_spacing)
						)
					)
					addOnLayoutChangeListener(spanResolver)
				}
			}
		}
	}

	private inner class SpanSizeLookup : GridLayoutManager.SpanSizeLookup() {

		init {
			isSpanIndexCacheEnabled = true
			isSpanGroupIndexCacheEnabled = true
		}

		override fun getSpanSize(position: Int): Int {
			val total =
				(binding.recyclerView.layoutManager as? GridLayoutManager)?.spanCount ?: return 1
			return when (listAdapter?.getItemViewType(position)) {
				MangaListAdapter.ITEM_TYPE_MANGA_GRID -> 1
				else -> total
			}
		}

		fun invalidateCache() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}
}