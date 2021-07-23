package org.koitharu.kotatsu.details.ui

import android.app.ActivityOptions
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.databinding.FragmentChaptersBinding
import org.koitharu.kotatsu.details.ui.adapter.BranchesAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersSelectionDecoration
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState

class ChaptersFragment : BaseFragment<FragmentChaptersBinding>(),
	OnListItemClickListener<MangaChapter>, ActionMode.Callback, AdapterView.OnItemSelectedListener {

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
			addItemDecoration(
				DividerItemDecoration(
					view.context,
					RecyclerView.VERTICAL
				)
			)
			addItemDecoration(selectionDecoration!!)
			setHasFixedSize(true)
			adapter = chaptersAdapter
		}
		val branchesAdapter = BranchesAdapter()
		binding.spinnerBranches.adapter = branchesAdapter
		binding.spinnerBranches.onItemSelectedListener = this

		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.chapters.observe(viewLifecycleOwner, this::onChaptersChanged)
		viewModel.branches.observe(viewLifecycleOwner) {
			branchesAdapter.setItems(it)
			binding.spinnerBranches.isVisible = it.size > 1
		}
		viewModel.selectedBranchIndex.observe(viewLifecycleOwner) {
			if (it != -1 && it != binding.spinnerBranches.selectedItemPosition) {
				binding.spinnerBranches.setSelection(it)
			}
		}
		viewModel.isChaptersReversed.observe(viewLifecycleOwner) {
			activity?.invalidateOptionsMenu()
		}
	}

	override fun onDestroyView() {
		chaptersAdapter = null
		selectionDecoration = null
		binding.spinnerBranches.adapter = null
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

	override fun onItemClick(item: MangaChapter, view: View) {
		if (selectionDecoration?.checkedItemsCount != 0) {
			selectionDecoration?.toggleItemChecked(item.id)
			if (selectionDecoration?.checkedItemsCount == 0) {
				actionMode?.finish()
			} else {
				actionMode?.invalidate()
				binding.recyclerViewChapters.invalidateItemDecorations()
			}
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
				ReaderState(item.id, 0, 0)
			), options.toBundle()
		)
	}

	override fun onItemLongClick(item: MangaChapter, view: View): Boolean {
		if (actionMode == null) {
			actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
		}
		return actionMode?.also {
			selectionDecoration?.setItemIsChecked(item.id, true)
			binding.recyclerViewChapters.invalidateItemDecorations()
			it.invalidate()
		} != null
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_save -> {
				DownloadService.start(
					context ?: return false,
					viewModel.manga.value ?: return false,
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
		viewModel.setSelectedBranch(binding.spinnerBranches.selectedItem as String?)
	}

	override fun onNothingSelected(parent: AdapterView<*>?) = Unit

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		val manga = viewModel.manga.value
		mode.menuInflater.inflate(R.menu.mode_chapters, menu)
		menu.findItem(R.id.action_save).isVisible = manga?.source != MangaSource.LOCAL
		mode.title = manga?.title
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		val count = selectionDecoration?.checkedItemsCount ?: return false
		mode.subtitle = resources.getQuantityString(
			R.plurals.chapters_from_x,
			count,
			count,
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
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom + binding.spinnerBranches.height
		)
	}

	private fun onChaptersChanged(list: List<ChapterListItem>) {
		chaptersAdapter?.items = list
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible = isLoading
	}
}