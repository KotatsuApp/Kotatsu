package org.koitharu.kotatsu.download.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemStorageConfigBinding
import org.koitharu.kotatsu.settings.storage.DirectoryModel

class DestinationsAdapter(context: Context, dataset: List<DirectoryModel>) :
	ArrayAdapter<DirectoryModel>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, dataset) {

	init {
		setDropDownViewResource(R.layout.item_storage_config)
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView ?: LayoutInflater.from(parent.context)
			.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
		val item = getItem(position) ?: return view
		view.findViewById<TextView>(android.R.id.text1).text = item.title ?: view.context.getString(item.titleRes)
		return view
	}

	override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView ?: LayoutInflater.from(parent.context)
			.inflate(R.layout.item_storage_config, parent, false)
		val item = getItem(position) ?: return view
		val binding =
			view.tag as? ItemStorageConfigBinding ?: ItemStorageConfigBinding.bind(view).also { view.tag = it }
		binding.buttonRemove.isVisible = false
		binding.textViewTitle.text = item.title ?: view.context.getString(item.titleRes)
		binding.textViewSubtitle.textAndVisible = item.file?.path
		return view
	}
}
