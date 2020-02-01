package org.koitharu.kotatsu.ui.main.details

import android.os.Bundle
import androidx.core.view.isVisible
import coil.api.load
import kotlinx.android.synthetic.main.fragment_details.*
import moxy.ktx.moxyPresenter
import org.koin.core.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.utils.ext.setChips

class MangaDetailsFragment : BaseFragment(R.layout.fragment_details), MangaDetailsView {

	private val presenter by moxyPresenter { (activity as MangaDetailsActivity).presenter }

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
	}

	override fun onMangaUpdated(manga: Manga) {
		imageView_cover.load(manga.largeCoverUrl ?: manga.coverUrl)
		textView_title.text = manga.title
		textView_subtitle.text = manga.localizedTitle
		textView_description.text = manga.description
		chips_tags.setChips(manga.tags) {
			create(
				text = it.title,
				iconRes = R.drawable.ic_chip_tag,
				tag = it
			)
		}
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onError(e: Exception) {

	}
}