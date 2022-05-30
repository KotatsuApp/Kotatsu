package org.koitharu.kotatsu.local.ui

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ActionMode
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.progress.Progress

class LocalListFragment : MangaListFragment(), ActivityResultCallback<List<@JvmSuppressWildcards Uri>> {

	override val viewModel by viewModel<LocalListViewModel>()
	private val importCall = registerForActivityResult(
		ActivityResultContracts.OpenMultipleDocuments(),
		this
	)
	private var importSnackbar: Snackbar? = null
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
			IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE)
		)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		addMenuProvider(LocalListMenuProvider(this::onEmptyActionClick))
		viewModel.onMangaRemoved.observe(viewLifecycleOwner) { onItemRemoved() }
		viewModel.importProgress.observe(viewLifecycleOwner, ::onImportProgressChanged)
	}

	override fun onDestroyView() {
		importSnackbar = null
		super.onDestroyView()
	}

	override fun onDetach() {
		requireContext().unregisterReceiver(downloadReceiver)
		super.onDetach()
	}

	override fun onScrolledToEnd() = Unit

	override fun onEmptyActionClick() {
		try {
			importCall.launch(arrayOf("*/*"))
		} catch (e: ActivityNotFoundException) {
			e.printStackTraceDebug()
			Snackbar.make(
				binding.recyclerView,
				R.string.operation_not_supported,
				Snackbar.LENGTH_SHORT
			).show()
		}
	}

	override fun onActivityResult(result: List<@JvmSuppressWildcards Uri>) {
		if (result.isEmpty()) return
		viewModel.importFiles(result)
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_local, menu)
		return super.onCreateActionMode(mode, menu)
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
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
			else -> super.onActionItemClicked(mode, item)
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

	private fun onImportProgressChanged(progress: Progress?) {
		if (progress == null) {
			importSnackbar?.dismiss()
			importSnackbar = null
			return
		}
		val summaryText = getString(
			R.string.importing_progress,
			progress.value + 1,
			progress.total,
		)
		importSnackbar?.setText(summaryText) ?: run {
			val snackbar =
				Snackbar.make(binding.recyclerView, summaryText, Snackbar.LENGTH_INDEFINITE)
			importSnackbar = snackbar
			snackbar.show()
		}
	}

	companion object {

		fun newInstance() = LocalListFragment()
	}
}