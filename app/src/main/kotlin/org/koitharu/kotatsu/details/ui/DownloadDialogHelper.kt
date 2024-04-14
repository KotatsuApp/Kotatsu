package org.koitharu.kotatsu.details.ui

import android.content.DialogInterface
import android.view.View
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.ids
import org.koitharu.kotatsu.core.ui.dialog.RecyclerViewAlertDialog
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.download.ui.dialog.DownloadOption
import org.koitharu.kotatsu.download.ui.dialog.downloadOptionAD
import org.koitharu.kotatsu.settings.SettingsActivity

class DownloadDialogHelper(
	private val host: View,
	private val viewModel: DetailsViewModel,
) {

	fun show(callback: OnListItemClickListener<DownloadOption>) {
		val branch = viewModel.selectedBranchValue
		val allChapters = viewModel.manga.value?.chapters ?: return
		val branchChapters = viewModel.manga.value?.getChapters(branch).orEmpty()
		val history = viewModel.history.value

		val options = buildList {
			add(DownloadOption.WholeManga(allChapters.ids()))
			if (branch != null && branchChapters.isNotEmpty()) {
				add(DownloadOption.AllChapters(branch, branchChapters.ids()))
			}

			if (history != null) {
				val unreadChapters = branchChapters.dropWhile { it.id != history.chapterId }
				if (unreadChapters.isNotEmpty() && unreadChapters.size < branchChapters.size) {
					add(DownloadOption.AllUnreadChapters(unreadChapters.ids(), branch))
					if (unreadChapters.size > 5) {
						add(DownloadOption.NextUnreadChapters(unreadChapters.take(5).ids()))
						if (unreadChapters.size > 10) {
							add(DownloadOption.NextUnreadChapters(unreadChapters.take(10).ids()))
						}
					}
				}
			} else {
				if (branchChapters.size > 5) {
					add(DownloadOption.FirstChapters(branchChapters.take(5).ids()))
					if (branchChapters.size > 10) {
						add(DownloadOption.FirstChapters(branchChapters.take(10).ids()))
					}
				}
			}
			add(DownloadOption.SelectionHint())
		}
		var dialog: DialogInterface? = null
		val listener = OnListItemClickListener<DownloadOption> { item, _ ->
			callback.onItemClick(item, host)
			dialog?.dismiss()
		}
		dialog = RecyclerViewAlertDialog.Builder<DownloadOption>(host.context)
			.addAdapterDelegate(downloadOptionAD(listener))
			.setCancelable(true)
			.setTitle(R.string.download)
			.setNegativeButton(android.R.string.cancel)
			.setNeutralButton(R.string.settings) { _, _ ->
				host.context.startActivity(SettingsActivity.newDownloadsSettingsIntent(host.context))
			}
			.setItems(options)
			.create()
			.also { it.show() }
	}
}
