package org.koitharu.kotatsu.ui.list.local

import android.content.ActivityNotFoundException
import android.net.Uri
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.list.MangaListFragment
import org.koitharu.kotatsu.utils.ext.ellipsize
import java.io.File

class LocalListFragment : MangaListFragment<File>(), ActivityResultCallback<Uri> {

	private val presenter by moxyPresenter(factory = ::LocalListPresenter)

	override fun onRequestMoreItems(offset: Int) {
		if (offset == 0) {
			presenter.loadList()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_local, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_import -> {
				try {
					registerForActivityResult(ActivityResultContracts.OpenDocument(), this)
						.launch(arrayOf("*/*"))
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
			presenter.importFile(context?.applicationContext ?: return, result)
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
						presenter.delete(data)
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
				true
			}
			else -> super.onPopupMenuItemSelected(item, data)
		}
	}

	override fun onItemRemoved(item: Manga) {
		super.onItemRemoved(item)
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