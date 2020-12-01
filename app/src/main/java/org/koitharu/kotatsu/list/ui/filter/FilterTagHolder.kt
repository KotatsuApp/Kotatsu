package org.koitharu.kotatsu.list.ui.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.databinding.ItemCheckableSingleBinding

class FilterTagHolder(parent: ViewGroup) :
	BaseViewHolder<MangaTag?, Boolean, ItemCheckableSingleBinding>(
		ItemCheckableSingleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
	) {

	override fun onBind(data: MangaTag?, extra: Boolean) {
		binding.radio.text = data?.title ?: context.getString(R.string.all)
		binding.radio.isChecked = extra
	}
}