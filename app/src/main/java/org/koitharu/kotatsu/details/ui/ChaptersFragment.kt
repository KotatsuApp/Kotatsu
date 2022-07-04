package org.koitharu.kotatsu.details.ui

import android.app.ActivityOptions
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.FragmentChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.BranchesAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersSelectionDecoration
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.local.ui.LocalChaptersRemoveService
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.RecyclerViewScrollCallback
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import kotlin.math.roundToInt

class ChaptersFragment :
	BaseFragment<FragmentChaptersBinding>(),
	OnListItemClickListener<ChapterListItem>,
	AdapterView.OnItemSelectedListener,
	MenuItem.OnActionExpandListener,
	SearchView.OnQueryTextListener,
	ListSelectionController.Callback {

	private val viewModel by sharedViewModel<DetailsViewModel>()

	private var chaptersAdapter: ChaptersAdapter? = null
	private var selectionController: ListSelectionController? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentChaptersBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		chaptersAdapter = ChaptersAdapter(this)
		selectionController = ListSelectionController(
			activity = requireActivity(),
			decoration = ChaptersSelectionDecoration(view.context),
			registryOwner = this,
			callback = this,
		)
		with(binding.recyclerViewChapters) {
			checkNotNull(selectionController).attachToRecyclerView(this)
			setHasFixedSize(true)
			adapter = chaptersAdapter
		}
		binding.spinnerBranches?.let(::initSpinner)
		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.chapters.observe(viewLifecycleOwner, this::onChaptersChanged)
		viewModel.isChaptersReversed.observe(viewLifecycleOwner) {
			activity?.invalidateOptionsMenu()
		}
		viewModel.isChaptersEmpty.observe(viewLifecycleOwner) {
			binding.textViewHolder.isVisible = it
			activity?.invalidateOptionsMenu()
		}
		addMenuProvider(ChaptersMenuProvider())
	}

	override fun onDestroyView() {
		chaptersAdapter = null
		selectionController = null
		binding.spinnerBranches?.adapter = null
		super.onDestroyView()
	}

	override fun onItemClick(item: ChapterListItem, view: View) {
		if (selectionController?.onItemClick(item.chapter.id) == true) {
			return
		}
		if (item.hasFlag(ChapterListItem.FLAG_MISSING)) {
			(activity as? DetailsActivity)?.showChapterMissingDialog(item.chapter.id)
			return
		}
		val options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.width, view.height)
		startActivity(
			ReaderActivity.newIntent(
				context = view.context,
				manga = viewModel.manga.value ?: return,
				state = ReaderState(item.chapter.id, 0, 0),
			),
			options.toBundle()
		)
	}

	override fun onItemLongClick(item: ChapterListItem, view: View): Boolean {
		return selectionController?.onItemLongClick(item.chapter.id) ?: false
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_save -> {
				DownloadService.start(
					context ?: return false,
					viewModel.getRemoteManga() ?: viewModel.manga.value ?: return false,
					selectionController?.snapshot(),
				)
				mode.finish()
				true
			}
			R.id.action_delete -> {
				val ids = selectionController?.peekCheckedIds()
				val manga = viewModel.manga.value
				when {
					ids.isNullOrEmpty() || manga == null -> Unit
					ids.size == manga.chapters?.size -> viewModel.deleteLocal()
					else -> {
						LocalChaptersRemoveService.start(requireContext(), manga, ids)
						Snackbar.make(
							binding.recyclerViewChapters,
							R.string.chapters_will_removed_background,
							Snackbar.LENGTH_LONG
						).show()
					}
				}
				mode.finish()
				true
			}
			R.id.action_select_all -> {
				val ids = chaptersAdapter?.items?.map { it.chapter.id } ?: return false
				selectionController?.addAll(ids)
				true
			}
			else -> false
		}
	}

	override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
		val spinner = binding.spinnerBranches ?: return
		viewModel.setSelectedBranch(spinner.selectedItem as String?)
	}

	override fun onNothingSelected(parent: AdapterView<*>?) = Unit

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_chapters, menu)
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		val selectedIds = selectionController?.peekCheckedIds() ?: return false
		val items = chaptersAdapter?.items?.filter { x -> x.chapter.id in selectedIds }.orEmpty()
		menu.findItem(R.id.action_save).isVisible = items.none { x ->
			x.chapter.source == MangaSource.LOCAL
		}
		menu.findItem(R.id.action_delete).isVisible = items.all { x ->
			x.chapter.source == MangaSource.LOCAL
		}
		mode.title = items.size.toString()
		return true
	}

	override fun onSelectionChanged(count: Int) {
		binding.recyclerViewChapters.invalidateItemDecorations()
	}

	override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true

	override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
		(item?.actionView as? SearchView)?.setQuery("", false)
		viewModel.performChapterSearch(null)
		return true
	}

	override fun onQueryTextSubmit(query: String?): Boolean = false

	override fun onQueryTextChange(newText: String?): Boolean {
		viewModel.performChapterSearch(newText)
		return true
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerViewChapters.updatePadding(
			bottom = insets.bottom + (binding.spinnerBranches?.height ?: 0),
		)
	}

	private fun initSpinner(spinner: Spinner) {
		val branchesAdapter = BranchesAdapter()
		spinner.adapter = branchesAdapter
		spinner.onItemSelectedListener = this
		viewModel.branches.observe(viewLifecycleOwner) {
			branchesAdapter.setItems(it)
			spinner.isVisible = it.size > 1
		}
		viewModel.selectedBranchIndex.observe(viewLifecycleOwner) {
			if (it != -1 && it != spinner.selectedItemPosition) {
				spinner.setSelection(it)
			}
		}
	}

	private fun onChaptersChanged(list: List<ChapterListItem>) {
		val adapter = chaptersAdapter ?: return
		if (adapter.itemCount == 0) {
			val position = list.indexOfFirst { it.hasFlag(ChapterListItem.FLAG_CURRENT) } - 1
			if (position > 0) {
				val offset = (resources.getDimensionPixelSize(R.dimen.chapter_list_item_height) * 0.6).roundToInt()
				adapter.setItems(list, RecyclerViewScrollCallback(binding.recyclerViewChapters, position, offset))
			} else {
				adapter.items = list
			}
		} else {
			adapter.items = list
		}
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible = isLoading
	}

	private inner class ChaptersMenuProvider : MenuProvider {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.opt_chapters, menu)
			val searchMenuItem = menu.findItem(R.id.action_search)
			searchMenuItem.setOnActionExpandListener(this@ChaptersFragment)
			val searchView = searchMenuItem.actionView as SearchView
			searchView.setOnQueryTextListener(this@ChaptersFragment)
			searchView.setIconifiedByDefault(false)
			searchView.queryHint = searchMenuItem.title
		}

		override fun onPrepareMenu(menu: Menu) {
			menu.findItem(R.id.action_reversed).isChecked = viewModel.isChaptersReversed.value == true
			menu.findItem(R.id.action_search).isVisible = viewModel.isChaptersEmpty.value == false
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
			R.id.action_reversed -> {
				viewModel.setChaptersReversed(!menuItem.isChecked)
				true
			}
			else -> false
		}
	}
}