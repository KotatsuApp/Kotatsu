package org.koitharu.kotatsu.settings.storage.directories

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemStorageConfigBinding
import org.koitharu.kotatsu.settings.storage.DirectoryModel

fun directoryConfigAD(
	clickListener: OnListItemClickListener<DirectoryModel>,
) = adapterDelegateViewBinding<DirectoryModel, DirectoryModel, ItemStorageConfigBinding>(
	{ layoutInflater, parent -> ItemStorageConfigBinding.inflate(layoutInflater, parent, false) },
) {

	binding.imageViewRemove.setOnClickListener { v -> clickListener.onItemClick(item, v) }

	bind {
		binding.textViewTitle.text = item.title ?: getString(item.titleRes)
		binding.textViewSubtitle.textAndVisible = item.file?.absolutePath
		binding.imageViewRemove.isVisible = item.isRemovable
		binding.imageViewRemove.isEnabled = !item.isChecked
		binding.textViewTitle.drawableStart = if (!item.isAvailable) {
			ContextCompat.getDrawable(context, R.drawable.ic_alert_outline)?.apply {
				setTint(ContextCompat.getColor(context, R.color.warning))
			}
		} else if (item.isChecked) {
			ContextCompat.getDrawable(context, R.drawable.ic_download)
		} else {
			null
		}
	}
}
