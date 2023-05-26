package org.koitharu.kotatsu.list.ui.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.LinearLayoutManager
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseBottomSheet
import org.koitharu.kotatsu.core.ui.util.CollapseActionViewCallback
import org.koitharu.kotatsu.core.util.ext.parentFragmentViewModels
import org.koitharu.kotatsu.databinding.SheetFilterBinding
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel

class FilterBottomSheet :
	BaseBottomSheet<SheetFilterBinding>(),
	MenuItem.OnActionExpandListener,
	SearchView.OnQueryTextListener,
	AsyncListDiffer.ListListener<FilterItem> {

	private val viewModel by parentFragmentViewModels<RemoteListViewModel>()
	private var collapsibleActionViewCallback: CollapseActionViewCallback? = null

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetFilterBinding {
		return SheetFilterBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetFilterBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = FilterAdapter(viewModel, this)
		binding.recyclerView.adapter = adapter
		viewModel.filterItems.observe(viewLifecycleOwner, adapter::setItems)
		initOptionsMenu()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		collapsibleActionViewCallback = null
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

	override fun onQueryTextSubmit(query: String?): Boolean = false

	override fun onQueryTextChange(newText: String?): Boolean {
		viewModel.filterSearch(newText?.trim().orEmpty())
		return true
	}

	override fun onCurrentListChanged(previousList: MutableList<FilterItem>, currentList: MutableList<FilterItem>) {
		if (currentList.size > previousList.size && view != null) {
			(requireViewBinding().recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
		}
	}

	private fun initOptionsMenu() {
		requireViewBinding().headerBar.inflateMenu(R.menu.opt_filter)
		val searchMenuItem = requireViewBinding().headerBar.menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
		collapsibleActionViewCallback = CollapseActionViewCallback(searchMenuItem).also {
			onBackPressedDispatcher.addCallback(it)
		}
	}

	companion object {

		private const val TAG = "FilterBottomSheet"

		fun show(fm: FragmentManager) = FilterBottomSheet().show(fm, TAG)
	}
}
