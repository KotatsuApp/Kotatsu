package org.koitharu.kotatsu.settings.sources.manage

import android.content.Intent
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
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.databinding.FragmentSettingsSourcesBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.sources.SourceSettingsFragment
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.catalog.SourcesCatalogActivity
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@AndroidEntryPoint
class SourcesManageFragment :
	BaseFragment<FragmentSettingsSourcesBinding>(),
	SourceConfigListener,
	RecyclerViewOwner {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var shortcutManager: AppShortcutManager

	private var reorderHelper: ItemTouchHelper? = null
	private var sourcesAdapter: SourceConfigAdapter? = null
	private val viewModel by viewModels<SourcesManageViewModel>()

	override val recyclerView: RecyclerView
		get() = requireViewBinding().recyclerView

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentSettingsSourcesBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(
		binding: FragmentSettingsSourcesBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		sourcesAdapter = SourceConfigAdapter(this, coil, viewLifecycleOwner)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = sourcesAdapter
			reorderHelper = ItemTouchHelper(SourcesReorderCallback()).also {
				it.attachToRecyclerView(this)
			}
		}
		viewModel.content.observe(viewLifecycleOwner, checkNotNull(sourcesAdapter))
		viewModel.onActionDone.observeEvent(
			viewLifecycleOwner,
			ReversibleActionObserver(binding.recyclerView),
		)
		addMenuProvider(SourcesMenuProvider())
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.manage_sources)
	}

	override fun onDestroyView() {
		sourcesAdapter = null
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
		(activity as? SettingsActivity)?.openFragment(
			fragmentClass = SourceSettingsFragment::class.java,
			args = Bundle(1).apply { putString(SourceSettingsFragment.EXTRA_SOURCE, item.source.name) },
			isFromRoot = false,
		)
	}

	override fun onItemLiftClick(item: SourceConfigItem.SourceItem) {
		viewModel.bringToTop(item.source)
	}

	override fun onItemShortcutClick(item: SourceConfigItem.SourceItem) {
		viewLifecycleScope.launch {
			shortcutManager.requestPinShortcut(item.source)
		}
	}

	override fun onItemPinClick(item: SourceConfigItem.SourceItem) {
		viewModel.setPinned(item.source, !item.isPinned)
	}

	override fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		viewModel.setEnabled(item.source, isEnabled)
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
			R.id.action_catalog -> {
				startActivity(Intent(context, SourcesCatalogActivity::class.java))
				true
			}

			R.id.action_disable_all -> {
				viewModel.disableAll()
				true
			}

			R.id.action_no_nsfw -> {
				settings.isNsfwContentDisabled = !menuItem.isChecked
				true
			}

			else -> false
		}

		override fun onPrepareMenu(menu: Menu) {
			super.onPrepareMenu(menu)
			menu.findItem(R.id.action_no_nsfw).isChecked = settings.isNsfwContentDisabled
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
		): Boolean = viewHolder.itemViewType == target.itemViewType

		override fun onMoved(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			fromPos: Int,
			target: RecyclerView.ViewHolder,
			toPos: Int,
			x: Int,
			y: Int,
		) {
			super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
			sourcesAdapter?.reorderItems(fromPos, toPos)
		}

		override fun canDropOver(
			recyclerView: RecyclerView,
			current: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = current.itemViewType == target.itemViewType && viewModel.canReorder(
			current.bindingAdapterPosition,
			target.bindingAdapterPosition,
		)

		override fun getDragDirs(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
		): Int {
			val item = viewHolder.getItem(SourceConfigItem.SourceItem::class.java)
			return if (item != null && item.isDraggable) {
				super.getDragDirs(recyclerView, viewHolder)
			} else {
				0
			}
		}

		override fun getSwipeDirs(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
		): Int {
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

		override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
			super.clearView(recyclerView, viewHolder)
			viewModel.saveSourcesOrder(sourcesAdapter?.items ?: return)
		}
	}
}
