package org.koitharu.kotatsu.ui.main.list

import android.view.ViewGroup
import coil.api.load
import coil.request.RequestDisposable
import kotlinx.android.synthetic.main.item_manga_list.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaInfo
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.textAndVisible

class MangaListHolder<E>(parent: ViewGroup) : BaseViewHolder<MangaInfo<E>>(parent, R.layout.item_manga_list) {

	private var coverRequest: RequestDisposable? = null

	override fun onBind(data: MangaInfo<E>) {
		coverRequest?.dispose()
		textView_title.text = data.manga.title
		textView_subtitle.textAndVisible = data.manga.localizedTitle
		coverRequest = imageView_cover.load(data.manga.coverUrl) {
			crossfade(true)
		}
	}
}