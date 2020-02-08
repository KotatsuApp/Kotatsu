package org.koitharu.kotatsu.ui.details

import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import coil.api.load
import kotlinx.android.synthetic.main.fragment_details.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.main.list.favourites.categories.FavouriteCategoriesDialog
import org.koitharu.kotatsu.ui.reader.ReaderActivity
import org.koitharu.kotatsu.utils.ext.setChips
import kotlin.math.roundToInt

class MangaDetailsFragment : BaseFragment(R.layout.fragment_details), MangaDetailsView {

	@Suppress("unused")
	private val presenter by moxyPresenter { (activity as MangaDetailsActivity).presenter }

	private var manga: Manga? = null
	private var history: MangaHistory? = null

	override fun onMangaUpdated(manga: Manga) {
		this.manga = manga
		imageView_cover.load(manga.largeCoverUrl ?: manga.coverUrl)
		textView_title.text = manga.title
		textView_subtitle.text = manga.altTitle
		textView_description.text = manga.description?.parseAsHtml()
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
		imageView_favourite.setOnClickListener {
			FavouriteCategoriesDialog.show(childFragmentManager, manga)
		}
		updateReadButton()
	}

	override fun onHistoryChanged(history: MangaHistory?) {
		this.history = history
		updateReadButton()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onError(e: Exception) {

	}

	private fun updateReadButton() {
		if (manga?.chapters.isNullOrEmpty()) {
			button_read.isEnabled = false
		} else {
			button_read.isEnabled = true
			if (history == null) {
				button_read.setText(R.string.read)
				button_read.setIconResource(R.drawable.ic_read)
			} else {
				button_read.setText(R.string.continue_)
				button_read.setIconResource(R.drawable.ic_play)
			}
			button_read.setOnClickListener {
				startActivity(
					ReaderActivity.newIntent(
						context ?: return@setOnClickListener,
						manga ?: return@setOnClickListener,
						history
					)
				)
			}
		}
	}
}