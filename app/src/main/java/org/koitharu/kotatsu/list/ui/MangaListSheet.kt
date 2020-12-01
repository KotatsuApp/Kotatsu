package org.koitharu.kotatsu.list.ui

import android.content.SharedPreferences
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
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.databinding.SheetListBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.utils.UiUtils
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListSheet : BaseBottomSheet<SheetListBinding>(),
	PaginationScrollListener.Callback, OnListItemClickListener<Manga>,
	SharedPreferences.OnSharedPreferenceChangeListener, Toolbar.OnMenuItemClickListener {

	private val settings by inject<AppSettings>()

	private var adapter: MangaListAdapter? = null

	protected abstract val viewModel: MangaListViewModel

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetListBinding {
		return SheetListBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaListAdapter(get(), viewLifecycleOwner, this)
		initListMode(settings.listMode)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.addOnScrollListener(PaginationScrollListener(4, this))
		settings.subscribe(this)
		binding.toolbar.inflateMenu(R.menu.opt_list_sheet)
		binding.toolbar.setOnMenuItemClickListener(this)
		binding.toolbar.setNavigationOnClickListener {
			dismiss()
		}
		if (dialog !is BottomSheetDialog) {
			binding.toolbar.isVisible = true
			binding.textViewTitle.isVisible = false
			binding.appbar.elevation = resources.getDimension(R.dimen.elevation_large)
		}
		if (savedInstanceState == null) {
			onScrolledToEnd()
		}
		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.listMode.observe(viewLifecycleOwner, ::initListMode)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		adapter = null
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

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		when (key) {
			AppSettings.KEY_LIST_MODE -> initListMode(settings.listMode)
			AppSettings.KEY_GRID_SIZE -> UiUtils.SpanCountResolver.update(binding.recyclerView)
		}
	}

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	private fun onListChanged(list: List<Any>) {
		adapter?.items = list
		binding.textViewHolder.isVisible = list.isEmpty()
		binding.recyclerView.callOnScrollListeners()
	}

	private fun onError(e: Throwable) {
		Snackbar.make(binding.recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT)
			.show()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible = isLoading && !binding.recyclerView.hasItems
		if (isLoading) {
			binding.textViewHolder.isVisible = false
		}
	}

	private fun initListMode(mode: ListMode) {
		val ctx = context ?: return
		val position = binding.recyclerView.firstItem
		binding.recyclerView.layoutManager = null
		binding.recyclerView.clearItemDecorations()
		binding.recyclerView.removeOnLayoutChangeListener(UiUtils.SpanCountResolver)
		binding.recyclerView.layoutManager = when (mode) {
			ListMode.GRID -> {
				GridLayoutManager(ctx, UiUtils.resolveGridSpanCount(ctx)).apply {
					spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
						override fun getSpanSize(position: Int) = if (position < TODO() as Int)
							1 else this@apply.spanCount
					}
				}
			}
			else -> LinearLayoutManager(ctx)
		}
		binding.recyclerView.addItemDecoration(
			when (mode) {
				ListMode.LIST -> DividerItemDecoration(ctx, RecyclerView.VERTICAL)
				ListMode.DETAILED_LIST,
				ListMode.GRID -> SpacingItemDecoration(
					resources.getDimensionPixelOffset(R.dimen.grid_spacing)
				)
			}
		)
		if (mode == ListMode.GRID) {
			binding.recyclerView.addOnLayoutChangeListener(UiUtils.SpanCountResolver)
		}
		adapter?.notifyDataSetChanged()
		binding.recyclerView.firstItem = position
	}
}