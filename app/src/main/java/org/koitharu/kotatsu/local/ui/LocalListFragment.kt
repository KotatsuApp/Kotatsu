package org.koitharu.kotatsu.local.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_list.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ext.ellipsize

class LocalListFragment : MangaListFragment(), ActivityResultCallback<Uri> {

	override val viewModel by viewModel<LocalListViewModel>()
	private val importCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
		this
	)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.onMangaRemoved.observe(viewLifecycleOwner, ::onItemRemoved)
	}

	override fun onRefresh() {
		super.onRefresh()
		viewModel.onRefresh()
	}

	override fun onScrolledToEnd() = Unit

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_local, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_import -> {
				try {
					importCall.launch(arrayOf("*/*"))
				} catch (e: ActivityNotFoundException) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace()
					}
					Snackbar.make(
						recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT
					).show()
				}
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun getTitle(): CharSequence? {
		return getString(R.string.local_storage)
	}

	override fun setUpEmptyListHolder() {
		textView_holder.setText(R.string.text_local_holder)
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			viewModel.importFile(result)
		}
	}

	override fun onCreatePopupMenu(inflater: MenuInflater, menu: Menu, data: Manga) {
		super.onCreatePopupMenu(inflater, menu, data)
		inflater.inflate(R.menu.popup_local, menu)
	}

	override fun onPopupMenuItemSelected(item: MenuItem, data: Manga): Boolean {
		return when (item.itemId) {
			R.id.action_delete -> {
				AlertDialog.Builder(context ?: return false)
					.setTitle(R.string.delete_manga)
					.setMessage(getString(R.string.text_delete_local_manga, data.title))
					.setPositiveButton(R.string.delete) { _, _ ->
						viewModel.delete(data)
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
				true
			}
			else -> super.onPopupMenuItemSelected(item, data)
		}
	}

	private fun onItemRemoved(item: Manga) {
		Snackbar.make(
			recyclerView, getString(
				R.string._s_deleted_from_local_storage,
				item.title.ellipsize(16)
			), Snackbar.LENGTH_SHORT
		).show()
	}

	companion object {

		fun newInstance() = LocalListFragment()
	}
}