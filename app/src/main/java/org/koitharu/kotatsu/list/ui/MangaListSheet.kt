package org.koitharu.kotatsu.list.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.sheet_list.*
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.ProgressBarAdapter
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.utils.UiUtils
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListSheet : BaseBottomSheet(R.layout.sheet_list),
	PaginationScrollListener.Callback, OnRecyclerItemClickListener<Manga>,
	SharedPreferences.OnSharedPreferenceChangeListener, Toolbar.OnMenuItemClickListener {

	private val settings by inject<AppSettings>()
	private val adapterConfig = ConcatAdapter.Config.Builder()
		.setIsolateViewTypes(true)
		.setStableIdMode(ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS)
		.build()

	private var adapter: MangaListAdapter? = null
	private var progressAdapter: ProgressBarAdapter? = null

	protected abstract val viewModel: MangaListViewModel

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaListAdapter(this)
		progressAdapter = ProgressBarAdapter()
		initListMode(settings.listMode)
		recyclerView.adapter = adapter
		recyclerView.addOnScrollListener(PaginationScrollListener(4, this))
		settings.subscribe(this)
		toolbar.inflateMenu(R.menu.opt_list_sheet)
		toolbar.setOnMenuItemClickListener(this)
		toolbar.setNavigationOnClickListener {
			dismiss()
		}
		if (dialog !is BottomSheetDialog) {
			toolbar.isVisible = true
			textView_title.isVisible = false
			appbar.elevation = resources.getDimension(R.dimen.elevation_large)
		}
		if (savedInstanceState == null) {
			onRequestMoreItems(0)
		}
		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		adapter = null
		progressAdapter = null
		super.onDestroyView()
	}

	protected fun setTitle(title: CharSequence) {
		toolbar.title = title
		textView_title.text = title
	}

	protected fun setSubtitle(subtitle: CharSequence) {
		toolbar.subtitle = subtitle
	}

	override fun onCreateDialog(savedInstanceState: Bundle?) =
		super.onCreateDialog(savedInstanceState).also {
			val behavior = (it as? BottomSheetDialog)?.behavior ?: return@also
			behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
				private val elevation = resources.getDimension(R.dimen.elevation_large)

				override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_EXPANDED) {
						toolbar.isVisible = true
						textView_title.isVisible = false
						appbar.elevation = elevation
					} else {
						toolbar.isVisible = false
						textView_title.isVisible = true
						appbar.elevation = 0f
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
			AppSettings.KEY_GRID_SIZE -> UiUtils.SpanCountResolver.update(recyclerView)
		}
	}

	override fun onItemClick(item: Manga, position: Int, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	private fun onListChanged(list: List<Manga>) {
		adapter?.replaceData(list)
		textView_holder.isVisible = list.isEmpty()
		progressAdapter?.isProgressVisible = list.isNotEmpty()
		recyclerView.callOnScrollListeners()
	}

	override fun getItemsCount() = adapter?.itemCount ?: 0

	private fun onError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading && !recyclerView.hasItems
		if (isLoading) {
			textView_holder.isVisible = false
		}
	}

	private fun initListMode(mode: ListMode) {
		val ctx = context ?: return
		val position = recyclerView.firstItem
		recyclerView.adapter = null
		recyclerView.layoutManager = null
		recyclerView.clearItemDecorations()
		recyclerView.removeOnLayoutChangeListener(UiUtils.SpanCountResolver)
		adapter?.listMode = mode
		recyclerView.layoutManager = when (mode) {
			ListMode.GRID -> {
				GridLayoutManager(ctx, UiUtils.resolveGridSpanCount(ctx)).apply {
					spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
						override fun getSpanSize(position: Int) = if (position < getItemsCount())
							1 else this@apply.spanCount
					}
				}
			}
			else -> LinearLayoutManager(ctx)
		}
		recyclerView.adapter = ConcatAdapter(adapterConfig, adapter, progressAdapter)
		recyclerView.addItemDecoration(
			when (mode) {
				ListMode.LIST -> DividerItemDecoration(ctx, RecyclerView.VERTICAL)
				ListMode.DETAILED_LIST,
				ListMode.GRID -> SpacingItemDecoration(
					resources.getDimensionPixelOffset(R.dimen.grid_spacing)
				)
			}
		)
		if (mode == ListMode.GRID) {
			recyclerView.addOnLayoutChangeListener(UiUtils.SpanCountResolver)
		}
		adapter?.notifyDataSetChanged()
		recyclerView.firstItem = position
	}
}