package org.koitharu.kotatsu.scrobbling.common.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.ui.util.CollapseActionViewCallback
import org.koitharu.kotatsu.core.util.RecyclerViewScrollCallback
import org.koitharu.kotatsu.core.util.ext.consume
import org.koitharu.kotatsu.core.util.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.setProgressIcon
import org.koitharu.kotatsu.core.util.ext.setTabsEnabled
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.databinding.SheetScrobblingSelectorBinding
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.common.ui.selector.adapter.ScrobblerMangaSelectionDecoration
import org.koitharu.kotatsu.scrobbling.common.ui.selector.adapter.ScrobblerSelectorAdapter

@AndroidEntryPoint
class ScrobblingSelectorSheet :
	BaseAdaptiveSheet<SheetScrobblingSelectorBinding>(),
	OnListItemClickListener<ScrobblerManga>,
	PaginationScrollListener.Callback,
	View.OnClickListener,
	MenuItem.OnActionExpandListener,
	SearchView.OnQueryTextListener,
	TabLayout.OnTabSelectedListener,
	ListStateHolderListener,
	AsyncListDiffer.ListListener<ListModel> {

	private var collapsibleActionViewCallback: CollapseActionViewCallback? = null
	private var paginationScrollListener: PaginationScrollListener? = null
	private val viewModel by viewModels<ScrobblingSelectorViewModel>()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetScrobblingSelectorBinding {
		return SheetScrobblingSelectorBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetScrobblingSelectorBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		disableFitToContents()
		val listAdapter = ScrobblerSelectorAdapter(this, this)
		listAdapter.addListListener(this)
		val decoration = ScrobblerMangaSelectionDecoration(binding.root.context)
		with(binding.recyclerView) {
			adapter = listAdapter
			addItemDecoration(decoration)
			addItemDecoration(TypedListSpacingDecoration(context, false))
			addOnScrollListener(
				PaginationScrollListener(4, this@ScrobblingSelectorSheet).also {
					paginationScrollListener = it
				},
			)
		}
		binding.buttonDone.setOnClickListener(this)
		initOptionsMenu()
		initTabs()

		viewModel.content.observe(viewLifecycleOwner, listAdapter)
		viewModel.selectedItemId.observe(viewLifecycleOwner) {
			decoration.checkedItemId = it
			binding.recyclerView.invalidateItemDecorations()
		}
		viewModel.onError.observeEvent(viewLifecycleOwner, ::onError)
		viewModel.onClose.observeEvent(viewLifecycleOwner) {
			dismiss()
		}
		viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
			binding.buttonDone.isEnabled = !isLoading
			if (isLoading) {
				binding.buttonDone.setProgressIcon()
			} else {
				binding.buttonDone.setIconResource(R.drawable.ic_check)
			}
			binding.tabs.setTabsEnabled(!isLoading)
		}
		viewModel.selectedScrobblerIndex.observe(viewLifecycleOwner) { index ->
			val tab = binding.tabs.getTabAt(index)
			if (tab != null && !tab.isSelected) {
				tab.select()
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		collapsibleActionViewCallback = null
		paginationScrollListener = null
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val typeMask = WindowInsetsCompat.Type.systemBars()
		val basePadding = v.resources.getDimensionPixelOffset(R.dimen.list_spacing_normal)
		viewBinding?.recyclerView?.updatePadding(
			bottom = basePadding + insets.getInsets(typeMask).bottom,
		)
		return insets.consume(v, typeMask, bottom = true)
	}

	override fun onCurrentListChanged(previousList: MutableList<ListModel>, currentList: MutableList<ListModel>) {
		if (previousList.singleOrNull() is LoadingFooter) {
			val rv = viewBinding?.recyclerView ?: return
			val selectedId = viewModel.selectedItemId.value
			val target = if (selectedId == NO_ID) {
				0
			} else {
				currentList.indexOfFirst { it is ScrobblerManga && it.id == selectedId }.coerceAtLeast(0)
			}
			rv.post(RecyclerViewScrollCallback(rv, target, if (target == 0) 0 else rv.height / 3))
			paginationScrollListener?.postInvalidate(rv)
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> viewModel.onDoneClick()
		}
	}

	override fun onItemClick(item: ScrobblerManga, view: View) {
		viewModel.selectItem(item.id)
	}

	override fun onRetryClick(error: Throwable) {
		if (ExceptionResolver.canResolve(error)) {
			viewLifecycleScope.launch {
				if (exceptionResolver.resolve(error)) {
					viewModel.retry()
				}
			}
		} else {
			viewModel.retry()
		}
	}

	override fun onEmptyActionClick() {
		openSearch()
	}

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		setExpanded(isExpanded = true, isLocked = true)
		collapsibleActionViewCallback?.onMenuItemActionExpand(item)
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		val searchView = (item.actionView as? SearchView) ?: return false
		searchView.setQuery("", false)
		searchView.post { setExpanded(isExpanded = false, isLocked = false) }
		collapsibleActionViewCallback?.onMenuItemActionCollapse(item)
		return true
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		if (query == null || query.length < 3) {
			return false
		}
		viewModel.search(query)
		requireViewBinding().toolbar.menu.findItem(R.id.action_search)?.collapseActionView()
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean = false

	override fun onTabSelected(tab: TabLayout.Tab) {
		viewModel.setScrobblerIndex(tab.position)
	}

	override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

	override fun onTabReselected(tab: TabLayout.Tab?) {
		if (!isExpanded) {
			setExpanded(isExpanded = true, isLocked = behavior?.isDraggable == false)
		}
		requireViewBinding().recyclerView.firstVisibleItemPosition = 0
	}

	private fun openSearch() {
		val menuItem = requireViewBinding().toolbar.menu.findItem(R.id.action_search) ?: return
		menuItem.expandActionView()
	}

	private fun onError(e: Throwable) {
		Toast.makeText(requireContext(), e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
		if (viewModel.isEmpty) {
			dismissAllowingStateLoss()
		}
	}

	private fun initOptionsMenu() {
		requireViewBinding().toolbar.inflateMenu(R.menu.opt_shiki_selector)
		val searchMenuItem = requireViewBinding().toolbar.menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
		collapsibleActionViewCallback = CollapseActionViewCallback(searchMenuItem).also {
			onBackPressedDispatcher.addCallback(it)
		}
	}

	private fun initTabs() {
		val entries = viewModel.availableScrobblers
		val tabs = requireViewBinding().tabs
		val selectedId = arguments?.getInt(AppRouter.KEY_ID, -1) ?: -1
		tabs.removeAllTabs()
		tabs.clearOnTabSelectedListeners()
		tabs.addOnTabSelectedListener(this)
		for (entry in entries) {
			val tab = tabs.newTab()
			tab.tag = entry.scrobblerService
			tab.setIcon(entry.scrobblerService.iconResId)
			tab.setText(entry.scrobblerService.titleResId)
			tabs.addTab(tab)
			if (entry.scrobblerService.id == selectedId) {
				tab.select()
			}
		}
	}
}
