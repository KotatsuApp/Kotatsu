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
import org.koitharu.kotatsu.utils.ext.longHashCode
import java.io.File

class StorageSelectDialog private constructor(private val delegate: AlertDialog) :
	DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context, defaultValue: File?, listener: OnStorageSelectListener) {

		private val adapter = VolumesAdapter(context)
		private val delegate = AlertDialog.Builder(context)

		init {
			if (adapter.isEmpty) {
				delegate.setMessage(R.string.cannot_find_available_storage)
			} else {
				val checked = adapter.volumes.indexOfFirst {
					it.first.canonicalPath == defaultValue?.canonicalPath
				}
				delegate.setSingleChoiceItems(adapter, checked) { d, i ->
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

		val volumes = getAvailableVolumes(context)

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val view = convertView ?: parent.inflate(R.layout.item_storage)
			val item = volumes[position]
			val binding = ItemStorageBinding.bind(view)
			binding.textViewTitle.text = item.second
			binding.textViewSubtitle.text = item.first.path
			return view
		}

		override fun getItem(position: Int): Pair<File, String> = volumes[position]

		override fun getItemId(position: Int) = volumes[position].first.absolutePath.longHashCode()

		override fun getCount() = volumes.size

	}

	fun interface OnStorageSelectListener {

		fun onStorageSelected(file: File)
	}

	private companion object {

		fun getAvailableVolumes(context: Context): List<Pair<File, String>> {
			return LocalMangaRepository.getAvailableStorageDirs(context).map {
				it to it.getStorageName(context)
			}
		}
	}
}