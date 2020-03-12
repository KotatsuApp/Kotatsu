package org.koitharu.kotatsu.ui.common.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.item_storage.view.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.findParent
import org.koitharu.kotatsu.utils.ext.inflate
import org.koitharu.kotatsu.utils.ext.longHashCode
import java.io.File

class StorageSelectDialog private constructor(private val delegate: AlertDialog) :
	DialogInterface by delegate {

	fun show() = delegate.show()

	class Builder(context: Context) {

		private val delegate = AlertDialog.Builder(context)
			.setAdapter(VolumesAdapter(context)) { _, _ ->

			}

		fun setTitle(@StringRes titleResId: Int): Builder {
			delegate.setTitle(titleResId)
			return this
		}

		fun setTitle(title: CharSequence): Builder {
			delegate.setTitle(title)
			return this
		}

		fun create() = StorageSelectDialog(delegate.create())
	}

	private class VolumesAdapter(context: Context): BaseAdapter() {

		private val volumes = getAvailableVolumes(context)

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val view = convertView ?: parent.inflate(R.layout.item_storage)
			val item = volumes[position]
			view.textView_title.text = item.second
			view.textView_subtitle.text = item.first.path
			return view
		}

		override fun getItem(position: Int): Any = volumes[position]

		override fun getItemId(position: Int) = volumes[position].first.absolutePath.longHashCode()

		override fun getCount() = volumes.size

	}

	private companion object {

		@JvmStatic
		fun getAvailableVolumes(context: Context): List<Pair<File,String>> = context.getExternalFilesDirs(null).mapNotNull {
			val root = it.findParent { x -> x.name == "Android" }?.parentFile ?: return@mapNotNull null
			root to when {
				Environment.isExternalStorageEmulated(root) -> context.getString(R.string.internal_storage)
				Environment.isExternalStorageRemovable(root) -> context.getString(R.string.external_storage)
				else -> root.name
			}
		}
	}
}