package org.koitharu.kotatsu.base.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemStorageBinding
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.utils.ext.getStorageName
import org.koitharu.kotatsu.utils.ext.inflate
import java.io.File

class StorageSelectDialog private constructor(private val delegate: AlertDialog) :
	DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context, defaultValue: File?, listener: OnStorageSelectListener) {

		private val adapter = VolumesAdapter(context)
		private val delegate = MaterialAlertDialogBuilder(context)

		init {
			if (adapter.isEmpty) {
				delegate.setMessage(R.string.cannot_find_available_storage)
			} else {
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

	private class VolumesAdapter(context: Context) : BaseAdapter() {

		var selectedItemPosition: Int = -1
		val volumes = getAvailableVolumes(context)

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val view = convertView ?: parent.inflate(R.layout.item_storage)
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

		private fun getAvailableVolumes(context: Context): List<Pair<File, String>> {
			return LocalMangaRepository.getAvailableStorageDirs(context).map {
				it to it.getStorageName(context)
			}
		}
	}

	fun interface OnStorageSelectListener {

		fun onStorageSelected(file: File)
	}
}