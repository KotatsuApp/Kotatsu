package org.koitharu.kotatsu.settings.backup

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.setChecked
import org.koitharu.kotatsu.databinding.ItemCheckableMultipleBinding
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback.Companion.PAYLOAD_CHECKED_CHANGED
import org.koitharu.kotatsu.list.ui.adapter.ListItemType

class BackupEntriesAdapter(
	clickListener: OnListItemClickListener<BackupEntryModel>,
) : BaseListAdapter<BackupEntryModel>() {

	init {
		addDelegate(ListItemType.NAV_ITEM, backupEntryAD(clickListener))
	}
}

private fun backupEntryAD(
	clickListener: OnListItemClickListener<BackupEntryModel>,
) = adapterDelegateViewBinding<BackupEntryModel, BackupEntryModel, ItemCheckableMultipleBinding>(
	{ layoutInflater, parent -> ItemCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v ->
		clickListener.onItemClick(item, v)
	}

	bind { payloads ->
		with(binding.root) {
			setText(item.titleResId)
			setChecked(item.isChecked, PAYLOAD_CHECKED_CHANGED in payloads)
			isEnabled = item.isEnabled
		}
	}
}
