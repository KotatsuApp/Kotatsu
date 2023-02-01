package org.koitharu.kotatsu.scrobbling.ui.selector

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import coil.ImageLoader
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.databinding.SheetScrobblingSelectorBinding
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.ui.selector.adapter.ShikiMangaSelectionDecoration
import org.koitharu.kotatsu.scrobbling.ui.selector.adapter.ShikimoriSelectorAdapter
import org.koitharu.kotatsu.utils.ext.assistedViewModels
import org.koitharu.kotatsu.utils.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.requireParcelable
import org.koitharu.kotatsu.utils.ext.withArgs
import javax.inject.Inject

@AndroidEntryPoint
class ScrobblingSelectorBottomSheet :
	BaseBottomSheet<SheetScrobblingSelectorBinding>(),
	OnListItemClickListener<ScrobblerManga>,
	PaginationScrollListener.Callback,
	View.OnClickListener,
	MenuItem.OnActionExpandListener,
	SearchView.OnQueryTextListener,
	DialogInterface.OnKeyListener,
	TabLayout.OnTabSelectedListener,
	ListStateHolderListener {

	@Inject
	lateinit var viewModelFactory: ScrobblingSelectorViewModel.Factory

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by assistedViewModels {
		viewModelFactory.create(
			requireArguments().requireParcelable<ParcelableManga>(MangaIntent.KEY_MANGA).manga,
		)
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetScrobblingSelectorBinding {
		return SheetScrobblingSelectorBinding.inflate(inflater, container, false)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return super.onCreateDialog(savedInstanceState).also {
			it.setOnKeyListener(this)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val listAdapter = ShikimoriSelectorAdapter(viewLifecycleOwner, coil, this, this)
		val decoration = ShikiMangaSelectionDecoration(view.context)
		with(binding.recyclerView) {
			adapter = listAdapter
			addItemDecoration(decoration)
			addOnScrollListener(PaginationScrollListener(4, this@ScrobblingSelectorBottomSheet))
		}
		binding.buttonDone.setOnClickListener(this)
		initOptionsMenu()
		initTabs()

		viewModel.content.observe(viewLifecycleOwner) { listAdapter.items = it }
		viewModel.selectedItemId.observe(viewLifecycleOwner) {
			decoration.checkedItemId = it
			binding.recyclerView.invalidateItemDecorations()
		}
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.onClose.observe(viewLifecycleOwner) {
			dismiss()
		}
		viewModel.searchQuery.observe(viewLifecycleOwner) {
			binding.headerBar.subtitle = it
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> viewModel.onDoneClick()
		}
	}

	override fun onItemClick(item: ScrobblerManga, view: View) {
		viewModel.selectedItemId.value = item.id
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() {
		openSearch()
	}

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		setExpanded(isExpanded = true, isLocked = true)
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		val searchView = (item.actionView as? SearchView) ?: return false
		searchView.setQuery("", false)
		searchView.post { setExpanded(isExpanded = false, isLocked = false) }
		return true
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		if (query == null || query.length < 3) {
			return false
		}
		viewModel.search(query)
		binding.headerBar.menu.findItem(R.id.action_search)?.collapseActionView()
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean = false

	override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			val menuItem = binding.headerBar.menu.findItem(R.id.action_search) ?: return false
			if (menuItem.isActionViewExpanded) {
				if (event?.action == KeyEvent.ACTION_UP) {
					menuItem.collapseActionView()
				}
				return true
			}
		}
		return false
	}

	override fun onTabSelected(tab: TabLayout.Tab) {
		viewModel.setScrobblerIndex(tab.position)
	}

	override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

	override fun onTabReselected(tab: TabLayout.Tab?) {
		if (!isExpanded) {
			setExpanded(isExpanded = true, isLocked = behavior?.isDraggable == false)
		}
		binding.recyclerView.firstVisibleItemPosition = 0
	}

	private fun openSearch() {
		val menuItem = binding.headerBar.menu.findItem(R.id.action_search) ?: return
		menuItem.expandActionView()
	}

	private fun onError(e: Throwable) {
		Toast.makeText(requireContext(), e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
		if (viewModel.isEmpty) {
			dismissAllowingStateLoss()
		}
	}

	private fun initOptionsMenu() {
		binding.headerBar.inflateMenu(R.menu.opt_shiki_selector)
		val searchMenuItem = binding.headerBar.menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
	}

	private fun initTabs() {
		val entries = viewModel.availableScrobblers
		val tabs = binding.tabs
		if (entries.size <= 1) {
			tabs.isVisible = false
			return
		}
		val selectedId = arguments?.getInt(ARG_SCROBBLER, -1) ?: -1
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
		tabs.isVisible = true
	}

	companion object {

		private const val TAG = "ScrobblingSelectorBottomSheet"
		private const val ARG_SCROBBLER = "scrobbler"

		fun show(fm: FragmentManager, manga: Manga, scrobblerService: ScrobblerService?) =
			ScrobblingSelectorBottomSheet().withArgs(2) {
				putParcelable(MangaIntent.KEY_MANGA, ParcelableManga(manga, withChapters = false))
				if (scrobblerService != null) {
					putInt(ARG_SCROBBLER, scrobblerService.id)
				}
			}.show(fm, TAG)
	}
}
