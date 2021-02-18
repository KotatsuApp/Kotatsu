package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil.ImageLoader
import coil.util.CoilUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.databinding.FragmentDetailsBinding
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesDialog
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.FileSizeUtils
import org.koitharu.kotatsu.utils.ext.*
import kotlin.math.roundToInt

class DetailsFragment : BaseFragment<FragmentDetailsBinding>(), View.OnClickListener,
	View.OnLongClickListener {

	private val viewModel by sharedViewModel<DetailsViewModel>()
	private val coil by inject<ImageLoader>()
	private var tagsJob: Job? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentDetailsBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.manga.observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.favouriteCategories.observe(viewLifecycleOwner, ::onFavouriteChanged)
		viewModel.readingHistory.observe(viewLifecycleOwner, ::onHistoryChanged)
	}

	private fun onMangaUpdated(manga: Manga) {
		with(binding) {
			imageViewCover.newImageRequest(manga.largeCoverUrl ?: manga.coverUrl)
				.referer(manga.publicUrl)
				.fallback(R.drawable.ic_placeholder)
				.placeholderMemoryCacheKey(CoilUtils.metadata(imageViewCover)?.memoryCacheKey)
				.lifecycle(viewLifecycleOwner)
				.enqueueWith(coil)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitle
			textViewDescription.text =
				manga.description?.parseAsHtml()?.takeUnless(Spanned::isBlank)
					?: getString(R.string.no_description)
			if (manga.rating == Manga.NO_RATING) {
				ratingBar.isVisible = false
			} else {
				ratingBar.progress = (ratingBar.max * manga.rating).roundToInt()
				ratingBar.isVisible = true
			}
			imageViewFavourite.setOnClickListener(this@DetailsFragment)
			buttonRead.setOnClickListener(this@DetailsFragment)
			buttonRead.setOnLongClickListener(this@DetailsFragment)
			buttonRead.isEnabled = !manga.chapters.isNullOrEmpty()
			bindTags(manga)
		}
	}

	private fun onHistoryChanged(history: MangaHistory?) {
		with(binding.buttonRead) {
			if (history == null) {
				setText(R.string.read)
				setIconResource(R.drawable.ic_read)
			} else {
				setText(R.string._continue)
				setIconResource(R.drawable.ic_play)
			}
		}
	}

	private fun onFavouriteChanged(isFavourite: Boolean) {
		binding.imageViewFavourite.setImageResource(
			if (isFavourite) {
				R.drawable.ic_heart
			} else {
				R.drawable.ic_heart_outline
			}
		)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible = isLoading
	}

	override fun onClick(v: View) {
		val manga = viewModel.manga.value
		when (v.id) {
			R.id.imageView_favourite -> {
				FavouriteCategoriesDialog.show(childFragmentManager, manga ?: return)
			}
			R.id.button_read -> {
				startActivity(
					ReaderActivity.newIntent(
						context ?: return,
						manga ?: return,
						null
					)
				)
			}
		}
	}

	override fun onLongClick(v: View): Boolean {
		when (v.id) {
			R.id.button_read -> {
				if (viewModel.readingHistory.value == null) {
					return false
				}
				v.showPopupMenu(R.menu.popup_read) {
					when (it.itemId) {
						R.id.action_read -> {
							startActivity(
								ReaderActivity.newIntent(
									context ?: return@showPopupMenu false,
									viewModel.manga.value ?: return@showPopupMenu false,
									viewModel.chapters.value?.firstOrNull()?.let { c ->
										ReaderState(c.chapter.id, 0, 0)
									}
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

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom
		)
	}

	private fun bindTags(manga: Manga) {
		tagsJob?.cancel()
		tagsJob = viewLifecycleScope.launch {
			val tags = ArrayList<ChipsView.ChipModel>(manga.tags.size + 2)
			if (manga.author != null) {
				tags += ChipsView.ChipModel(
					title = manga.author,
					icon = R.drawable.ic_chip_user
				)
			}
			for (tag in manga.tags) {
				tags += ChipsView.ChipModel(
					title = tag.title,
					icon = R.drawable.ic_chip_tag
				)
			}
			val file = manga.url.toUri().toFileOrNull()
			if (file != null) {
				val size = withContext(Dispatchers.IO) {
					file.length()
				}
				tags += ChipsView.ChipModel(
					title = FileSizeUtils.formatBytes(requireContext(), size),
					icon = R.drawable.ic_chip_storage
				)
			} else {
				tags += ChipsView.ChipModel(
					title = manga.source.title,
					icon = R.drawable.ic_chip_web
				)
			}
			binding.chipsTags.setChips(tags)
		}
	}
}