package org.koitharu.kotatsu.details.ui

import android.app.ActivityOptions
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.FragmentChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.BranchesAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersSelectionDecoration
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.RecyclerViewScrollCallback

class ChaptersFragment : BaseFragment<FragmentChaptersBinding>(),
	OnListItemClickListener<ChapterListItem>,
	ActionMode.Callback,
	AdapterView.OnItemSelectedListener {

	private val viewModel by sharedViewModel<DetailsViewModel>()

	private var chaptersAdapter: ChaptersAdapter? = null
	private var actionMode: ActionMode? = null
	private var selectionDecoration: ChaptersSelectionDecoration? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentChaptersBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		chaptersAdapter = ChaptersAdapter(this)
		selectionDecoration = ChaptersSelectionDecoration(view.context)
		with(binding.recyclerViewChapters) {
			addItemDecoration(selectionDecoration!!)
			setHasFixedSize(true)
			adapter = chaptersAdapter
		}
		binding.spinnerBranches?.let(::initSpinner)
		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.chapters.observe(viewLifecycleOwner, this::onChaptersChanged)
		viewModel.isChaptersReversed.observe(viewLifecycleOwner) {
			activity?.invalidateOptionsMenu()
		}
	}

	override fun onDestroyView() {
		chaptersAdapter = null
		selectionDecoration = null
		binding.spinnerBranches?.adapter = null
		super.onDestroyView()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_chapters, menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		super.onPrepareOptionsMenu(menu)
		menu.findItem(R.id.action_reversed).isChecked = viewModel.isChaptersReversed.value == true
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_reversed -> {
			viewModel.setChaptersReversed(!item.isChecked)
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onItemClick(item: ChapterListItem, view: View) {
		if (selectionDecoration?.checkedItemsCount != 0) {
			selectionDecoration?.toggleItemChecked(item.chapter.id)
			if (selectionDecoration?.checkedItemsCount == 0) {
				actionMode?.finish()
			} else {
				actionMode?.invalidate()
				binding.recyclerViewChapters.invalidateItemDecorations()
			}
			return
		}
		if (item.hasFlag(ChapterListItem.FLAG_MISSING)) {
			(activity as? DetailsActivity)?.showChapterMissingDialog(item.chapter.id)
			return
		}
		val options = ActivityOptions.makeScaleUpAnimation(
			view,
			0,
			0,
			view.measuredWidth,
			view.measuredHeight
		)
		startActivity(
			ReaderActivity.newIntent(
				view.context,
				viewModel.manga.value ?: return,
				ReaderState(item.chapter.id, 0, 0)
			), options.toBundle()
		)
	}

	override fun onItemLongClick(item: ChapterListItem, view: View): Boolean {
		if (actionMode == null) {
			actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
		}
		return actionMode?.also {
			selectionDecoration?.setItemIsChecked(item.chapter.id, true)
			binding.recyclerViewChapters.invalidateItemDecorations()
			it.invalidate()
		} != null
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_save -> {
				DownloadService.start(
					context ?: return false,
					viewModel.getRemoteManga() ?: viewModel.manga.value ?: return false,
					selectionDecoration?.checkedItemsIds
				)
				mode.finish()
				true
			}
			R.id.action_select_all -> {
				val ids = chaptersAdapter?.items?.map { it.chapter.id } ?: return false
				selectionDecoration?.checkAll(ids)
				binding.recyclerViewChapters.invalidateItemDecorations()
				mode.invalidate()
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
		val manga = viewModel.manga.value
		mode.menuInflater.inflate(R.menu.mode_chapters, menu)
		mode.title = manga?.title
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		val selectedIds = selectionDecoration?.checkedItemsIds ?: return false
		val items = chaptersAdapter?.items?.filter { x -> x.chapter.id in selectedIds }.orEmpty()
		menu.findItem(R.id.action_save).isVisible = items.none { x ->
			x.chapter.source == MangaSource.LOCAL
		}
		mode.subtitle = resources.getQuantityString(
			R.plurals.chapters_from_x,
			items.size,
			items.size,
			chaptersAdapter?.itemCount ?: 0
		)
		return true
	}

	override fun onDestroyActionMode(mode: ActionMode?) {
		selectionDecoration?.clearSelection()
		binding.recyclerViewChapters.invalidateItemDecorations()
		actionMode = null
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
				adapter.setItems(list, RecyclerViewScrollCallback(binding.recyclerViewChapters, position))
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
}