package org.koitharu.kotatsu.download.ui.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.Px
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.ActivityDownloadsBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.worker.PausingReceiver
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsActivity : BaseActivity<ActivityDownloadsBinding>(),
	DownloadItemListener,
	ListSelectionController.Callback2 {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<DownloadsViewModel>()
	private lateinit var selectionController: ListSelectionController

	@Px
	private var listSpacing = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDownloadsBinding.inflate(layoutInflater))
		listSpacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val downloadsAdapter = DownloadsAdapter(this, coil, this)
		val decoration = SpacingItemDecoration(listSpacing)
		selectionController = ListSelectionController(
			activity = this,
			decoration = DownloadsSelectionDecoration(this),
			registryOwner = this,
			callback = this,
		)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			addItemDecoration(decoration)
			adapter = downloadsAdapter
			selectionController.attachToRecyclerView(this)
		}
		addMenuProvider(DownloadsMenuProvider(this, viewModel))
		viewModel.items.observe(this) {
			downloadsAdapter.items = it
		}
		val menuObserver = Observer<Any> { _ -> invalidateOptionsMenu() }
		viewModel.hasActiveWorks.observe(this, menuObserver)
		viewModel.hasPausedWorks.observe(this, menuObserver)
		viewModel.hasCancellableWorks.observe(this, menuObserver)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			left = insets.left + listSpacing,
			right = insets.right + listSpacing,
			bottom = insets.bottom,
		)
		binding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onItemClick(item: DownloadItemModel, view: View) {
		if (selectionController.onItemClick(item.id.mostSignificantBits)) {
			return
		}
		startActivity(DetailsActivity.newIntent(view.context, item.manga))
	}

	override fun onItemLongClick(item: DownloadItemModel, view: View): Boolean {
		return selectionController.onItemLongClick(item.id.mostSignificantBits)
	}

	override fun onCancelClick(item: DownloadItemModel) {
		viewModel.cancel(item.id)
	}

	override fun onPauseClick(item: DownloadItemModel) {
		sendBroadcast(PausingReceiver.getPauseIntent(item.id))
	}

	override fun onResumeClick(item: DownloadItemModel) {
		sendBroadcast(PausingReceiver.getResumeIntent(item.id))
	}

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		binding.recyclerView.invalidateItemDecorations()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_downloads, menu)
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_resume -> {
				viewModel.resume(controller.snapshot())
				mode.finish()
				true
			}

			R.id.action_pause -> {
				viewModel.pause(controller.snapshot())
				mode.finish()
				true
			}

			R.id.action_cancel -> {
				viewModel.cancel(controller.snapshot())
				mode.finish()
				true
			}

			R.id.action_remove -> {
				viewModel.remove(controller.snapshot())
				mode.finish()
				true
			}

			R.id.action_select_all -> {
				controller.addAll(viewModel.allIds())
				true
			}

			else -> false
		}
	}

	override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		val snapshot = viewModel.snapshot(controller.peekCheckedIds())
		var canPause = true
		var canResume = true
		var canCancel = true
		var canRemove = true
		for (item in snapshot) {
			canPause = canPause and item.canPause
			canResume = canResume and item.canResume
			canCancel = canCancel and !item.workState.isFinished
			canRemove = canRemove and item.workState.isFinished
		}
		menu.findItem(R.id.action_pause)?.isVisible = canPause
		menu.findItem(R.id.action_resume)?.isVisible = canResume
		menu.findItem(R.id.action_cancel)?.isVisible = canCancel
		menu.findItem(R.id.action_remove)?.isVisible = canRemove
		return super.onPrepareActionMode(controller, mode, menu)
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, DownloadsActivity::class.java)
	}
}
