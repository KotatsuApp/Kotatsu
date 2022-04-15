package org.koitharu.kotatsu.base.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemStorageBinding
import org.koitharu.kotatsu.local.data.LocalStorageManager
import java.io.File

class StorageSelectDialog private constructor(private val delegate: AlertDialog) :
	DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context, storageManager: LocalStorageManager, listener: OnStorageSelectListener) {

		private val adapter = VolumesAdapter(storageManager)
		private val delegate = MaterialAlertDialogBuilder(context)

		init {
			if (adapter.isEmpty) {
				delegate.setMessage(R.string.cannot_find_available_storage)
			} else {
				val defaultValue = runBlocking {
					storageManager.getDefaultWriteableDir()
				}
				adapter.selectedItemPosition = adapter.volumes.indexOfFirst {
					it.first.canonicalPath == defaultValue?.canonicalPath
				}
				delegate.setAdapter(adapter) { d, i ->
					listener.onStorageSelected(adapter.getItem(i).first)
					d.dismiss()
				}
			}
		}

		fun setTitle(@StringRes titleResId: Int): Builder {
			delegate.setTitle(titleResId)
			return this
		}

		fun setTitle(title: CharSequence): Builder {
			delegate.setTitle(title)
			return this
		}

		fun setNegativeButton(@StringRes textId: Int): Builder {
			delegate.setNegativeButton(textId, null)
			return this
		}

		fun create() = StorageSelectDialog(delegate.create())
	}

	private class VolumesAdapter(storageManager: LocalStorageManager) : BaseAdapter() {

		var selectedItemPosition: Int = -1
		val volumes = getAvailableVolumes(storageManager)

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_storage, parent, false)
			val binding = (view.tag as? ItemStorageBinding) ?: ItemStorageBinding.bind(view).also {
				view.tag = it
			}
			val item = volumes[position]
			binding.imageViewIndicator.isChecked = selectedItemPosition == position
			binding.textViewTitle.text = item.second
			binding.textViewSubtitle.text = item.first.path
			return view
		}

		override fun getItem(position: Int): Pair<File, String> = volumes[position]

		override fun getItemId(position: Int) = position.toLong()

		override fun getCount() = volumes.size

		override fun hasStableIds() = true

		private fun getAvailableVolumes(storageManager: LocalStorageManager): List<Pair<File, String>> {
			return runBlocking {
				storageManager.getWriteableDirs().map {
					it to storageManager.getStorageDisplayName(it)
				}
			}
		}
	}

	fun interface OnStorageSelectListener {

		fun onStorageSelected(file: File)
	}
}