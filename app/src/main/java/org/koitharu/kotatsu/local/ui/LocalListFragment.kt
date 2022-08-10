package org.koitharu.kotatsu.local.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.addMenuProvider

class LocalListFragment : MangaListFragment() {

	override val viewModel by viewModels<LocalListViewModel>()
	private val downloadReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action == DownloadService.ACTION_DOWNLOAD_COMPLETE) {
				viewModel.onRefresh()
			}
		}
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		context.registerReceiver(
			downloadReceiver,
			IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE),
		)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		addMenuProvider(LocalListMenuProvider(this::onEmptyActionClick))
		viewModel.onMangaRemoved.observe(viewLifecycleOwner) { onItemRemoved() }
	}

	override fun onDetach() {
		requireContext().unregisterReceiver(downloadReceiver)
		super.onDetach()
	}

	override fun onEmptyActionClick() {
		ImportDialogFragment.show(childFragmentManager)
	}

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_local, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				showDeletionConfirm(selectedItemsIds, mode)
				true
			}
			R.id.action_share -> {
				val files = selectedItems.map { it.url.toUri().toFile() }
				ShareHelper(requireContext()).shareCbz(files)
				mode.finish()
				true
			}
			else -> super.onActionItemClicked(controller, mode, item)
		}
	}

	private fun showDeletionConfirm(ids: Set<Long>, mode: ActionMode) {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.delete_manga)
			.setMessage(getString(R.string.text_delete_local_manga_batch))
			.setPositiveButton(R.string.delete) { _, _ ->
				viewModel.delete(ids)
				mode.finish()
			}
			.setNegativeButton(android.R.string.cancel, null)
			.show()
	}

	private fun onItemRemoved() {
		Snackbar.make(binding.recyclerView, R.string.removal_completed, Snackbar.LENGTH_SHORT).show()
	}

	companion object {

		fun newInstance() = LocalListFragment()
	}
}
