package org.koitharu.kotatsu.ui.details

import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import coil.api.load
import kotlinx.android.synthetic.main.fragment_details.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaInfo
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.reader.ReaderActivity
import org.koitharu.kotatsu.utils.ext.setChips
import kotlin.math.roundToInt

class MangaDetailsFragment : BaseFragment(R.layout.fragment_details), MangaDetailsView {

	@Suppress("unused")
	private val presenter by moxyPresenter { (activity as MangaDetailsActivity).presenter }

	override fun onMangaUpdated(data: MangaInfo<MangaHistory?>) {
		imageView_cover.load(data.manga.largeCoverUrl ?: data.manga.coverUrl)
		textView_title.text = data.manga.title
		textView_subtitle.text = data.manga.localizedTitle
		textView_description.text = data.manga.description?.parseAsHtml()
		if (data.manga.rating == Manga.NO_RATING) {
			ratingBar.isVisible = false
		} else {
			ratingBar.progress = (ratingBar.max * data.manga.rating).roundToInt()
			ratingBar.isVisible = true
		}
		chips_tags.setChips(data.manga.tags) {
			create(
				text = it.title,
				iconRes = R.drawable.ic_chip_tag,
				tag = it
			)
		}
		if (data.manga.chapters.isNullOrEmpty()) {
			button_read.isEnabled = false
		} else {
			button_read.isEnabled = true
			if (data.extra == null) {
				button_read.setText(R.string.read)
				button_read.setIconResource(R.drawable.ic_read)
			} else {
				button_read.setText(R.string.continue_)
				button_read.setIconResource(R.drawable.ic_play)
			}
			val chapterId = data.extra?.chapterId ?: data.manga.chapters.first().id
			button_read.setOnClickListener {
				startActivity(
					ReaderActivity.newIntent(
						context ?: return@setOnClickListener,
						data.manga,
						chapterId
					)
				)
			}
		}
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onError(e: Exception) {

	}
}