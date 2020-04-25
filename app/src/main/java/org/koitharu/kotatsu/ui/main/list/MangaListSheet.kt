package org.koitharu.kotatsu.ui.main.list

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.sheet_list.*
import moxy.MvpDelegate
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.common.BaseBottomSheet
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.common.list.PaginationScrollListener
import org.koitharu.kotatsu.ui.common.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.UiUtils
import org.koitharu.kotatsu.utils.ext.*

abstract class MangaListSheet<E> : BaseBottomSheet(R.layout.sheet_list), MangaListView<E>,
	PaginationScrollListener.Callback, OnRecyclerItemClickListener<Manga>,
	SharedPreferences.OnSharedPreferenceChangeListener, Toolbar.OnMenuItemClickListener {

	private val settings by inject<AppSettings>()

	private var adapter: MangaListAdapter? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaListAdapter(this)
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
			// appbar.elevation = resources.getDimension(R.dimen.elevation_large)
		}
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		adapter = null
		super.onDestroyView()
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		if (savedInstanceState?.containsKey(MvpDelegate.MOXY_DELEGATE_TAGS_KEY) != true) {
			onRequestMoreItems(0)
		}
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
						// appbar.elevation = elevation
					} else {
						toolbar.isVisible = false
						textView_title.isVisible = true
						// appbar.elevation = 0f
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
			getString(R.string.key_list_mode) -> initListMode(settings.listMode)
			getString(R.string.key_grid_size) -> UiUtils.SpanCountResolver.update(recyclerView)
		}
	}

	override fun onItemClick(item: Manga, position: Int, view: View) {
		startActivity(MangaDetailsActivity.newIntent(context ?: return, item))
	}

	override fun onListChanged(list: List<Manga>) {
		adapter?.replaceData(list)
		textView_holder.isVisible = list.isEmpty()
		recyclerView.callOnScrollListeners()
	}

	override fun onListAppended(list: List<Manga>) {
		adapter?.appendData(list)
		if (list.isNotEmpty()) {
			textView_holder.isVisible = false
		}
		recyclerView.callOnScrollListeners()
	}

	override fun onListError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	override fun onInitFilter(
		sortOrders: List<SortOrder>,
		tags: List<MangaTag>,
		currentFilter: MangaFilter?
	) = Unit

	override fun onItemRemoved(item: Manga) {
		adapter?.let {
			it.removeItem(item)
			textView_holder.isGone = it.hasItems
		}
	}

	override fun onError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT).show()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
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
			ListMode.GRID -> GridLayoutManager(ctx, UiUtils.resolveGridSpanCount(ctx))
			else -> LinearLayoutManager(ctx)
		}
		recyclerView.adapter = adapter
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