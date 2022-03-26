package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.databinding.FragmentSettingsSourcesBinding
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourcesSettingsFragment : BaseFragment<FragmentSettingsSourcesBinding>(),
	SourceConfigListener,
	SearchView.OnQueryTextListener,
	MenuItem.OnActionExpandListener,
	RecyclerViewOwner {

	private var reorderHelper: ItemTouchHelper? = null
	private val viewModel by viewModel<SourcesSettingsViewModel>()

	override val recyclerView: RecyclerView
		get() = binding.recyclerView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentSettingsSourcesBinding.inflate(inflater, container, false)

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.remote_sources)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val sourcesAdapter = SourceConfigAdapter(this, get(), viewLifecycleOwner)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			// addItemDecoration(SourceConfigItemDecoration(view.context))
			adapter = sourcesAdapter
			reorderHelper = ItemTouchHelper(SourcesReorderCallback()).also {
				it.attachToRecyclerView(this)
			}
		}
		viewModel.items.observe(viewLifecycleOwner) {
			sourcesAdapter.items = it
		}
	}

	override fun onDestroyView() {
		reorderHelper = null
		super.onDestroyView()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_sources, menu)
		val searchMenuItem = menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
			left = insets.left,
			right = insets.right
		)
	}

	override fun onItemSettingsClick(item: SourceConfigItem.SourceItem) {
		(activity as? SettingsActivity)?.openMangaSourceSettings(item.source)
	}

	override fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		viewModel.setEnabled(item.source, isEnabled)
	}

	override fun onDragHandleTouch(holder: RecyclerView.ViewHolder) {
		reorderHelper?.startDrag(holder)
	}

	override fun onHeaderClick(header: SourceConfigItem.LocaleGroup) {
		viewModel.expandOrCollapse(header.localeId)
	}

	override fun onQueryTextSubmit(query: String?): Boolean = false

	override fun onQueryTextChange(newText: String?): Boolean {
		viewModel.performSearch(newText)
		return true
	}

	override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
		(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		(item.actionView as SearchView).setQuery("", false)
		return true
	}

	private inner class SourcesReorderCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		0,
	) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = viewHolder.itemViewType == target.itemViewType && viewModel.reorderSources(
			viewHolder.bindingAdapterPosition,
			target.bindingAdapterPosition,
		)

		override fun canDropOver(
			recyclerView: RecyclerView,
			current: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = current.itemViewType == target.itemViewType && viewModel.canReorder(
			current.bindingAdapterPosition,
			target.bindingAdapterPosition,
		)

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun isLongPressDragEnabled() = false
	}
}