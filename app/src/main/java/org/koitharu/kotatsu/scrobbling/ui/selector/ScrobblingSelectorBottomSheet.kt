package org.koitharu.kotatsu.scrobbling.ui.selector

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentManager
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.databinding.SheetScrobblingSelectorBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.ui.selector.adapter.ShikiMangaSelectionDecoration
import org.koitharu.kotatsu.scrobbling.ui.selector.adapter.ShikimoriSelectorAdapter
import org.koitharu.kotatsu.utils.BottomSheetToolbarController
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.withArgs

class ScrobblingSelectorBottomSheet :
	BaseBottomSheet<SheetScrobblingSelectorBinding>(),
	OnListItemClickListener<ScrobblerManga>,
	PaginationScrollListener.Callback,
	View.OnClickListener,
	MenuItem.OnActionExpandListener,
	SearchView.OnQueryTextListener,
	DialogInterface.OnKeyListener {

	private val viewModel by viewModel<ScrobblingSelectorViewModel> {
		parametersOf(requireNotNull(requireArguments().getParcelable<ParcelableManga>(MangaIntent.KEY_MANGA)).manga)
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
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		addBottomSheetCallback(BottomSheetToolbarController(binding.toolbar))
		val listAdapter = ShikimoriSelectorAdapter(viewLifecycleOwner, get(), this)
		val decoration = ShikiMangaSelectionDecoration(view.context)
		with(binding.recyclerView) {
			adapter = listAdapter
			addItemDecoration(decoration)
			addOnScrollListener(PaginationScrollListener(4, this@ScrobblingSelectorBottomSheet))
		}
		binding.buttonDone.setOnClickListener(this)
		initOptionsMenu()

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
			binding.toolbar.subtitle = it
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

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
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
		binding.toolbar.menu.findItem(R.id.action_search)?.collapseActionView()
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean = false

	override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			val menuItem = binding.toolbar.menu.findItem(R.id.action_search) ?: return false
			if (menuItem.isActionViewExpanded) {
				if (event?.action == KeyEvent.ACTION_UP) {
					menuItem.collapseActionView()
				}
				return true
			}
		}
		return false
	}

	private fun onError(e: Throwable) {
		Toast.makeText(requireContext(), e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
		if (viewModel.isEmpty) {
			dismissAllowingStateLoss()
		}
	}

	private fun initOptionsMenu() {
		binding.toolbar.inflateMenu(R.menu.opt_shiki_selector)
		val searchMenuItem = binding.toolbar.menu.findItem(R.id.action_search)
		searchMenuItem.setOnActionExpandListener(this)
		val searchView = searchMenuItem.actionView as SearchView
		searchView.setOnQueryTextListener(this)
		searchView.setIconifiedByDefault(false)
		searchView.queryHint = searchMenuItem.title
	}

	companion object {

		private const val TAG = "ScrobblingSelectorBottomSheet"

		fun show(fm: FragmentManager, manga: Manga) =
			ScrobblingSelectorBottomSheet().withArgs(1) {
				putParcelable(MangaIntent.KEY_MANGA, ParcelableManga(manga, withChapters = false))
			}.show(fm, TAG)
	}
}