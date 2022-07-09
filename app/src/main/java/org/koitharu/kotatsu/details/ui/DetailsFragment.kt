package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.adapter.BookmarksAdapter
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.databinding.FragmentDetailsBinding
import org.koitharu.kotatsu.details.ui.scrobbling.ScrobblingInfoBottomSheet
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.*

class DetailsFragment :
	BaseFragment<FragmentDetailsBinding>(),
	View.OnClickListener,
	View.OnLongClickListener,
	ChipsView.OnChipClickListener,
	OnListItemClickListener<Bookmark> {

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
		binding.scrobblingLayout.root.setOnClickListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethod.getInstance()
		binding.chipsTags.onChipClickListener = this
		viewModel.manga.observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.favouriteCategories.observe(viewLifecycleOwner, ::onFavouriteChanged)
		viewModel.readingHistory.observe(viewLifecycleOwner, ::onHistoryChanged)
		viewModel.bookmarks.observe(viewLifecycleOwner, ::onBookmarksChanged)
		viewModel.scrobblingInfo.observe(viewLifecycleOwner, ::onScrobblingInfoChanged)
		viewModel.description.observe(viewLifecycleOwner, ::onDescriptionChanged)
		addMenuProvider(DetailsMenuProvider())
	}

	override fun onItemClick(item: Bookmark, view: View) {
		startActivity(
			ReaderActivity.newIntent(view.context, item),
			scaleUpActivityOptionsOf(view).toBundle(),
		)
	}

	override fun onItemLongClick(item: Bookmark, view: View): Boolean {
		val menu = PopupMenu(view.context, view)
		menu.inflate(R.menu.popup_bookmark)
		menu.setOnMenuItemClickListener { menuItem ->
			when (menuItem.itemId) {
				R.id.action_remove -> viewModel.removeBookmark(item)
			}
			true
		}
		menu.show()
		return true
	}

	private fun onMangaUpdated(manga: Manga) {
		with(binding) {
			// Main
			loadCover(manga)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitle
			textViewAuthor.textAndVisible = manga.author
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

	private fun onDescriptionChanged(description: CharSequence?) {
		if (description.isNullOrBlank()) {
			binding.textViewDescription.setText(R.string.no_description)
		} else {
			binding.textViewDescription.text = description
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
		binding.progressView.setPercent(history?.percent ?: PROGRESS_NONE, animate = true)
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

	private fun onBookmarksChanged(bookmarks: List<Bookmark>) {
		var adapter = binding.recyclerViewBookmarks.adapter as? BookmarksAdapter
		binding.groupBookmarks.isGone = bookmarks.isEmpty()
		if (adapter != null) {
			adapter.items = bookmarks
		} else {
			adapter = BookmarksAdapter(coil, viewLifecycleOwner, this)
			adapter.items = bookmarks
			binding.recyclerViewBookmarks.adapter = adapter
			val spacing = resources.getDimensionPixelOffset(R.dimen.bookmark_list_spacing)
			binding.recyclerViewBookmarks.addItemDecoration(SpacingItemDecoration(spacing))
		}
	}

	private fun onScrobblingInfoChanged(scrobbling: ScrobblingInfo?) {
		with(binding.scrobblingLayout) {
			root.isVisible = scrobbling != null
			if (scrobbling == null) {
				CoilUtils.dispose(imageViewCover)
				return
			}
			imageViewCover.newImageRequest(scrobbling.coverUrl)
				.placeholder(R.drawable.ic_placeholder)
				.fallback(R.drawable.ic_placeholder)
				.error(R.drawable.ic_placeholder)
				.scale(Scale.FILL)
				.lifecycle(viewLifecycleOwner)
				.enqueueWith(coil)
			textViewTitle.text = scrobbling.title
			textViewTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, scrobbling.scrobbler.iconResId, 0)
			ratingBar.rating = scrobbling.rating * ratingBar.numStars
			textViewStatus.text = scrobbling.status?.let {
				resources.getStringArray(R.array.scrobbling_statuses).getOrNull(it.ordinal)
			}
		}
	}

	override fun onClick(v: View) {
		val manga = viewModel.manga.value ?: return
		when (v.id) {
			R.id.button_favorite -> {
				FavouriteCategoriesBottomSheet.show(childFragmentManager, manga)
			}
			R.id.scrobbling_layout -> {
				ScrobblingInfoBottomSheet.show(childFragmentManager)
			}
			R.id.button_read -> {
				val chapterId = viewModel.readingHistory.value?.chapterId
				if (chapterId != null && manga.chapters?.none { x -> x.id == chapterId } == true) {
					(activity as? DetailsActivity)?.showChapterMissingDialog(chapterId)
				} else {
					startActivity(
						ReaderActivity.newIntent(
							context = context ?: return,
							manga = manga,
							branch = viewModel.selectedBranchValue,
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
				startActivity(
					ImageActivity.newIntent(v.context, manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl }),
					scaleUpActivityOptionsOf(v).toBundle()
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
				val menu = PopupMenu(v.context, v)
				menu.inflate(R.menu.popup_read)
				menu.setOnMenuItemClickListener { menuItem ->
					when (menuItem.itemId) {
						R.id.action_read -> {
							val branch = viewModel.selectedBranchValue
							startActivity(
								ReaderActivity.newIntent(
									context = context ?: return@setOnMenuItemClickListener false,
									manga = viewModel.manga.value ?: return@setOnMenuItemClickListener false,
									state = viewModel.chapters.value?.firstOrNull { c ->
										c.chapter.branch == branch
									}?.let { c ->
										ReaderState(c.chapter.id, 0, 0)
									}
								)
							)
							true
						}
						else -> false
					}
				}
				menu.show()
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
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom
		)
	}

	private fun bindTags(manga: Manga) {
		binding.chipsTags.setChips(
			manga.tags.map { tag ->
				ChipsView.ChipModel(
					title = tag.title,
					icon = 0,
					data = tag,
					isCheckable = false,
					isChecked = false,
				)
			}
		)
	}

	private fun loadCover(manga: Manga) {
		val imageUrl = manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl }
		val lastResult = CoilUtils.result(binding.imageViewCover)
		if (lastResult?.request?.data == imageUrl) {
			return
		}
		val request = ImageRequest.Builder(context ?: return)
			.target(binding.imageViewCover)
			.data(imageUrl)
			.crossfade(context)
			.referer(manga.publicUrl)
			.lifecycle(viewLifecycleOwner)
		lastResult?.drawable?.let {
			request.fallback(it)
		} ?: request.fallback(R.drawable.ic_placeholder)
		request.enqueueWith(coil)
	}

	private inner class DetailsMenuProvider : MenuProvider {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.opt_details_info, menu)
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
			R.id.action_share -> {
				viewModel.manga.value?.let {
					val context = requireContext()
					if (it.source == MangaSource.LOCAL) {
						ShareHelper(context).shareCbz(listOf(it.url.toUri().toFile()))
					} else {
						ShareHelper(context).shareMangaLink(it)
					}
				}
				true
			}
			else -> false
		}
	}
}