package org.koitharu.kotatsu.details.ui

import android.app.ActivityOptions
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil.ImageLoader
import coil.request.ImageRequest
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.databinding.FragmentDetailsBinding
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesDialog
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.*

class DetailsFragment : BaseFragment<FragmentDetailsBinding>(), View.OnClickListener,
	View.OnLongClickListener, ChipsView.OnChipClickListener {

	private val viewModel by sharedViewModel<DetailsViewModel>()
	private val coil by inject<ImageLoader>(mode = LazyThreadSafetyMode.NONE)

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentDetailsBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.textViewAuthor.setOnClickListener(this)
		binding.buttonFavorite.setOnClickListener(this)
		binding.buttonRead.setOnClickListener(this)
		binding.buttonRead.setOnLongClickListener(this)
		binding.imageViewCover.setOnClickListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethod.getInstance()
		binding.chipsTags.onChipClickListener = this
		viewModel.manga.observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.favouriteCategories.observe(viewLifecycleOwner, ::onFavouriteChanged)
		viewModel.readingHistory.observe(viewLifecycleOwner, ::onHistoryChanged)
	}

	private fun onMangaUpdated(manga: Manga) {
		with(binding) {
			// Main
			loadCover(manga)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitle
			textViewAuthor.textAndVisible = manga.author
			textViewDescription.text = manga.description?.parseAsHtml()?.takeUnless(Spanned::isBlank)
				?: getString(R.string.no_description)
			when (manga.state) {
				MangaState.FINISHED -> {
					textViewState.apply {
						textAndVisible = resources.getString(R.string.state_finished)
						drawableStart = ContextCompat.getDrawable(context, R.drawable.ic_state_finished)
					}
				}
				MangaState.ONGOING -> {
					textViewState.apply {
						textAndVisible = resources.getString(R.string.state_ongoing)
						drawableStart = ContextCompat.getDrawable(context, R.drawable.ic_state_ongoing)
					}
				}
				else -> textViewState.isVisible = false
			}

			// Info containers
			val chapters = manga.chapters
			if (chapters.isNullOrEmpty()) {
				infoLayout.textViewChapters.isVisible = false
			} else {
				infoLayout.textViewChapters.isVisible = true
				infoLayout.textViewChapters.text = resources.getQuantityString(
					R.plurals.chapters,
					chapters.size,
					chapters.size,
				)
			}
			if (manga.hasRating) {
				infoLayout.textViewRating.text = String.format("%.1f", manga.rating * 5)
				infoLayout.ratingContainer.isVisible = true
			} else {
				infoLayout.ratingContainer.isVisible = false
			}
			if (manga.source == MangaSource.LOCAL) {
				infoLayout.textViewSource.isVisible = false
				val file = manga.url.toUri().toFileOrNull()
				if (file != null) {
					viewLifecycleScope.launch {
						val size = file.computeSize()
						infoLayout.textViewSize.text = FileSize.BYTES.format(requireContext(), size)
						infoLayout.textViewSize.isVisible = true
					}
				} else {
					infoLayout.textViewSize.isVisible = false
				}
			} else {
				infoLayout.textViewSource.text = manga.source.title
				infoLayout.textViewSource.isVisible = true
				infoLayout.textViewSize.isVisible = false
			}

			infoLayout.textViewNsfw.isVisible = manga.isNsfw

			// Buttons
			buttonRead.isEnabled = !manga.chapters.isNullOrEmpty()

			// Chips
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
		val iconRes = if (isFavourite) {
			R.drawable.ic_heart
		} else {
			R.drawable.ic_heart_outline
		}
		binding.buttonFavorite.setIconResource(iconRes)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		if (isLoading) {
			binding.progressBar.show()
		} else {
			binding.progressBar.hide()
		}
	}

	override fun onClick(v: View) {
		val manga = viewModel.manga.value ?: return
		when (v.id) {
			R.id.button_favorite -> {
				FavouriteCategoriesDialog.show(childFragmentManager, manga)
			}
			R.id.button_read -> {
				val chapterId = viewModel.readingHistory.value?.chapterId
				if (chapterId != null && manga.chapters?.none { x -> x.id == chapterId } == true) {
					(activity as? DetailsActivity)?.showChapterMissingDialog(chapterId)
				} else {
					startActivity(
						ReaderActivity.newIntent(
							context ?: return,
							manga,
							null
						)
					)
				}
			}
			R.id.textView_author -> {
				startActivity(
					SearchActivity.newIntent(
						context = v.context,
						source = manga.source,
						query = manga.author ?: return,
					)
				)
			}
			R.id.imageView_cover -> {
				val options = ActivityOptions.makeSceneTransitionAnimation(
					requireActivity(),
					binding.imageViewCover,
					binding.imageViewCover.transitionName,
				)
				startActivity(
					ImageActivity.newIntent(v.context, manga.largeCoverUrl ?: manga.coverUrl),
					options.toBundle()
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

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag ?: return
		startActivity(MangaListActivity.newIntent(requireContext(), setOf(tag)))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			bottom = insets.bottom,
		)
	}

	private fun bindTags(manga: Manga) {
		binding.chipsTags.setChips(
			manga.tags.map { tag ->
				ChipsView.ChipModel(
					title = tag.title,
					icon = 0,
					data = tag,
				)
			}
		)
	}

	private fun loadCover(manga: Manga) {
		val currentCover = binding.imageViewCover.drawable
		val request = ImageRequest.Builder(context ?: return)
			.target(binding.imageViewCover)
		if (currentCover != null) {
			request.data(manga.largeCoverUrl ?: return)
				.placeholderMemoryCacheKey(CoilUtils.metadata(binding.imageViewCover)?.memoryCacheKey)
				.fallback(currentCover)
		} else {
			request.crossfade(true)
				.data(manga.coverUrl)
				.fallback(R.drawable.ic_placeholder)
		}
		request.referer(manga.publicUrl)
			.lifecycle(viewLifecycleOwner)
			.enqueueWith(coil)
	}
}
