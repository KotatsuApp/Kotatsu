package org.koitharu.kotatsu.ui.main.list

import android.view.ViewGroup
import coil.api.load
import coil.request.RequestDisposable
import kotlinx.android.synthetic.main.item_manga_list_details.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.textAndVisible

class MangaListDetailsHolder(parent: ViewGroup) : BaseViewHolder<Manga>(parent, R.layout.item_manga_list_details) {

	private var coverRequest: RequestDisposable? = null

	override fun onBind(data: Manga) {
		coverRequest?.dispose()
		textView_title.text = data.title
		textView_subtitle.textAndVisible = data.localizedTitle
		coverRequest = imageView_cover.load(data.coverUrl) {
			crossfade(true)
		}
	}
}