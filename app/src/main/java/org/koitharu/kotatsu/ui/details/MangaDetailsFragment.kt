package org.koitharu.kotatsu.ui.details

import android.text.Spanned
import android.view.View
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import coil.api.load
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.fragment_details.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.main.list.favourites.categories.select.FavouriteCategoriesDialog
import org.koitharu.kotatsu.ui.reader.ReaderActivity
import org.koitharu.kotatsu.ui.search.MangaSearchSheet
import org.koitharu.kotatsu.utils.FileSizeUtils
import org.koitharu.kotatsu.utils.ext.addChips
import org.koitharu.kotatsu.utils.ext.showPopupMenu
import org.koitharu.kotatsu.utils.ext.textAndVisible
import org.koitharu.kotatsu.utils.ext.toFileOrNull
import kotlin.math.roundToInt

class MangaDetailsFragment : BaseFragment(R.layout.fragment_details), MangaDetailsView,
	View.OnClickListener,
	View.OnLongClickListener {

	@Suppress("unused")
	private val presenter by moxyPresenter(factory = MangaDetailsPresenter.Companion::getInstance)

	private var manga: Manga? = null
	private var history: MangaHistory? = null

	override fun onMangaUpdated(manga: Manga) {
		this.manga = manga
		imageView_cover.load(manga.largeCoverUrl ?: manga.coverUrl) {
			fallback(R.drawable.ic_placeholder)
			crossfade(true)
			lifecycle(this@MangaDetailsFragment)
		}
		textView_title.text = manga.title
		textView_subtitle.textAndVisible = manga.altTitle
		textView_description.text = manga.description?.parseAsHtml()?.takeUnless(Spanned::isBlank)
			?: getString(R.string.no_description)
		if (manga.rating == Manga.NO_RATING) {
			ratingBar.isVisible = false
		} else {
			ratingBar.progress = (ratingBar.max * manga.rating).roundToInt()
			ratingBar.isVisible = true
		}
		chips_tags.removeAllViews()
		manga.author?.let { a ->
			chips_tags.addChips(listOf(a)) {
				create(
					text = it,
					iconRes = R.drawable.ic_chip_user,
					tag = it,
					onClickListener = this@MangaDetailsFragment
				)
			}
		}
		chips_tags.addChips(manga.tags) {
			create(
				text = it.title,
				iconRes = R.drawable.ic_chip_tag,
				tag = it,
				onClickListener = this@MangaDetailsFragment
			)
		}
		manga.url.toUri().toFileOrNull()?.let { f ->
			chips_tags.addChips(listOf(f)) {
				create(
					text = FileSizeUtils.formatBytes(context, it.length()),
					iconRes = R.drawable.ic_chip_storage,
					tag = it,
					onClickListener = this@MangaDetailsFragment
				)
			}
		}
		imageView_favourite.setOnClickListener(this)
		button_read.setOnClickListener(this)
		button_read.setOnLongClickListener(this)
		updateReadButton()
	}

	override fun onHistoryChanged(history: MangaHistory?) {
		this.history = history
		updateReadButton()
	}

	override fun onFavouriteChanged(categories: List<FavouriteCategory>) {
		imageView_favourite.setImageResource(
			if (categories.isEmpty()) {
				R.drawable.ic_tag_heart_outline
			} else {
				R.drawable.ic_tag_heart
			}
		)
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onError(e: Throwable) = Unit //handled in activity

	override fun onMangaRemoved(manga: Manga) = Unit //handled in activity

	override fun onNewChaptersChanged(newChapters: Int) = Unit

	override fun onClick(v: View) {
		when {
			v.id == R.id.imageView_favourite -> {
				FavouriteCategoriesDialog.show(childFragmentManager, manga ?: return)
			}
			v.id == R.id.button_read -> {
				startActivity(
					ReaderActivity.newIntent(
						context ?: return,
						manga ?: return,
						history
					)
				)
			}
			v is Chip -> {
				when (val tag = v.tag) {
					is String -> MangaSearchSheet.show(activity?.supportFragmentManager
						?: childFragmentManager,
						manga?.source ?: return, tag)
				}
			}
		}
	}

	override fun onLongClick(v: View): Boolean {
		when (v.id) {
			R.id.button_read -> {
				if (history == null) {
					return false
				}
				v.showPopupMenu(R.menu.popup_read) {
					when (it.itemId) {
						R.id.action_read -> {
							startActivity(
								ReaderActivity.newIntent(
									context ?: return@showPopupMenu false,
									manga ?: return@showPopupMenu false
								)
							)
							true
						}
						else -> false
					}
				}
				return true
			}
			else -> return false
		}
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
				button_read.setText(R.string._continue)
				button_read.setIconResource(R.drawable.ic_play)
			}
		}
	}
}