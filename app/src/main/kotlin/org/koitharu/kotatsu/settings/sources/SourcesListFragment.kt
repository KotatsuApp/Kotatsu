package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.MenuProvider
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.FragmentSettingsSourcesBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@AndroidEntryPoint
class SourcesListFragment :
	BaseFragment<FragmentSettingsSourcesBinding>(),
	SourceConfigListener,
	RecyclerViewOwner {

	@Inject
	lateinit var coil: ImageLoader

	private var reorderHelper: ItemTouchHelper? = null
	private val viewModel by viewModels<SourcesListViewModel>()

	override val recyclerView: RecyclerView
		get() = requireViewBinding().recyclerView

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentSettingsSourcesBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentSettingsSourcesBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val sourcesAdapter = SourceConfigAdapter(this, coil, viewLifecycleOwner)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = sourcesAdapter
			reorderHelper = ItemTouchHelper(SourcesReorderCallback()).also {
				it.attachToRecyclerView(this)
			}
		}
		viewModel.items.observe(viewLifecycleOwner) {
			sourcesAdapter.items = it
		}
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(binding.recyclerView))
		addMenuProvider(SourcesMenuProvider())
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.remote_sources)
	}

	override fun onDestroyView() {
		reorderHelper = null
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		requireViewBinding().recyclerView.updatePadding(
			bottom = insets.bottom,
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onItemSettingsClick(item: SourceConfigItem.SourceItem) {
		val fragment = SourceSettingsFragment.newInstance(item.source)
		(activity as? SettingsActivity)?.openFragment(fragment, false)
	}

	override fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		viewModel.setEnabled(item.source, isEnabled)
	}

	override fun onHeaderClick(header: SourceConfigItem.LocaleGroup) {
		viewModel.expandOrCollapse(header.localeId)
	}

	override fun onCloseTip(tip: SourceConfigItem.Tip) {
		viewModel.onTipClosed(tip)
	}

	private inner class SourcesMenuProvider :
		MenuProvider,
		MenuItem.OnActionExpandListener,
		SearchView.OnQueryTextListener {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.opt_sources, menu)
			val searchMenuItem = menu.findItem(R.id.action_search)
			searchMenuItem.setOnActionExpandListener(this)
			val searchView = searchMenuItem.actionView as SearchView
			searchView.setOnQueryTextListener(this)
			searchView.setIconifiedByDefault(false)
			searchView.queryHint = searchMenuItem.title
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
			R.id.action_disable_all -> {
				viewModel.disableAll()
				true
			}

			else -> false
		}

		override fun onMenuItemActionExpand(item: MenuItem): Boolean {
			(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
			return true
		}

		override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
			(item.actionView as SearchView).setQuery("", false)
			return true
		}

		override fun onQueryTextSubmit(query: String?): Boolean = false

		override fun onQueryTextChange(newText: String?): Boolean {
			viewModel.performSearch(newText)
			return true
		}
	}

	private inner class SourcesReorderCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
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

		override fun getDragDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
			val item = viewHolder.getItem(SourceConfigItem.SourceItem::class.java)
			return if (item != null && item.isDraggable) {
				super.getDragDirs(recyclerView, viewHolder)
			} else {
				0
			}
		}

		override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
			val item = viewHolder.getItem(SourceConfigItem.Tip::class.java)
			return if (item != null) {
				super.getSwipeDirs(recyclerView, viewHolder)
			} else {
				0
			}
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			val item = viewHolder.getItem(SourceConfigItem.Tip::class.java)
			if (item != null) {
				viewModel.onTipClosed(item)
			}
		}

		override fun isLongPressDragEnabled() = true
	}
}
