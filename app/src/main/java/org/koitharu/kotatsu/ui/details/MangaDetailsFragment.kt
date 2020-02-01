package org.koitharu.kotatsu.ui.details

import androidx.core.view.isVisible
import coil.api.load
import kotlinx.android.synthetic.main.fragment_details.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.utils.ext.setChips
import kotlin.math.roundToInt

class MangaDetailsFragment : BaseFragment(R.layout.fragment_details), MangaDetailsView {

	@Suppress("unused")
	private val presenter by moxyPresenter { (activity as MangaDetailsActivity).presenter }

	override fun onMangaUpdated(manga: Manga) {
		imageView_cover.load(manga.largeCoverUrl ?: manga.coverUrl)
		textView_title.text = manga.title
		textView_subtitle.text = manga.localizedTitle
		textView_description.text = manga.description
		if (manga.rating == Manga.NO_RATING) {
			ratingBar.isVisible = false
		} else {
			ratingBar.progress = (ratingBar.max * manga.rating).roundToInt()
			ratingBar.isVisible = true
		}
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