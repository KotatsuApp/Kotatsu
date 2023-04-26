package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import coil.ImageLoader
import coil.request.ImageRequest
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.adapter.BookmarksAdapter
import org.koitharu.kotatsu.core.model.countChaptersByBranch
import org.koitharu.kotatsu.core.parser.MangaTagHighlighter
import org.koitharu.kotatsu.databinding.FragmentDetailsBinding
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.scrobbling.ScrobblingItemDecoration
import org.koitharu.kotatsu.details.ui.scrobbling.ScrollingInfoAdapter
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.main.ui.owners.NoModalBottomSheetOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.utils.FileSize
import org.koitharu.kotatsu.utils.ext.computeSize
import org.koitharu.kotatsu.utils.ext.crossfade
import org.koitharu.kotatsu.utils.ext.drawableTop
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.ifNullOrEmpty
import org.koitharu.kotatsu.utils.ext.measureHeight
import org.koitharu.kotatsu.utils.ext.resolveDp
import org.koitharu.kotatsu.utils.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.utils.ext.textAndVisible
import org.koitharu.kotatsu.utils.ext.toFileOrNull
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import org.koitharu.kotatsu.utils.image.CoverSizeResolver
import javax.inject.Inject

@AndroidEntryPoint
class DetailsFragment :
	BaseFragment<FragmentDetailsBinding>(),
	View.OnClickListener,
	ChipsView.OnChipClickListener,
	OnListItemClickListener<Bookmark> {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var tagHighlighter: MangaTagHighlighter

	private val viewModel by activityViewModels<DetailsViewModel>()

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentDetailsBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.textViewAuthor.setOnClickListener(this)
		binding.imageViewCover.setOnClickListener(this)
		binding.infoLayout.textViewSource.setOnClickListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethod.getInstance()
		binding.chipsTags.onChipClickListener = this
		viewModel.manga.observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.historyInfo.observe(viewLifecycleOwner, ::onHistoryChanged)
		viewModel.bookmarks.observe(viewLifecycleOwner, ::onBookmarksChanged)
		viewModel.scrobblingInfo.observe(viewLifecycleOwner, ::onScrobblingInfoChanged)
		viewModel.description.observe(viewLifecycleOwner, ::onDescriptionChanged)
		viewModel.chapters.observe(viewLifecycleOwner, ::onChaptersChanged)
	}

	override fun onItemClick(item: Bookmark, view: View) {
		startActivity(
			ReaderActivity.newIntent(view.context, item),
			scaleUpActivityOptionsOf(view).toBundle(),
		)
		Toast.makeText(view.context, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
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
			if (manga.hasRating) {
				ratingBar.rating = manga.rating * ratingBar.numStars
				ratingBar.isVisible = true
			} else {
				ratingBar.isVisible = false
			}

			when (manga.state) {
				MangaState.FINISHED -> {
					infoLayout.textViewState.apply {
						textAndVisible = resources.getString(R.string.state_finished)
						drawableTop = ContextCompat.getDrawable(context, R.drawable.ic_state_finished)
					}
				}

				MangaState.ONGOING -> {
					infoLayout.textViewState.apply {
						textAndVisible = resources.getString(R.string.state_ongoing)
						drawableTop = ContextCompat.getDrawable(context, R.drawable.ic_state_ongoing)
					}
				}

				else -> infoLayout.textViewState.isVisible = false
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

			// Chips
			bindTags(manga)
		}
	}

	private fun onChaptersChanged(chapters: List<ChapterListItem>?) {
		val infoLayout = binding.infoLayout
		if (chapters.isNullOrEmpty()) {
			infoLayout.textViewChapters.isVisible = false
		} else {
			val count = chapters.countChaptersByBranch()
			infoLayout.textViewChapters.isVisible = true
			infoLayout.textViewChapters.text = resources.getQuantityString(R.plurals.chapters, count, count)
		}
	}

	private fun onDescriptionChanged(description: CharSequence?) {
		if (description.isNullOrBlank()) {
			binding.textViewDescription.setText(R.string.no_description)
		} else {
			binding.textViewDescription.text = description
		}
	}

	private fun onHistoryChanged(history: HistoryInfo) {
		binding.progressView.setPercent(history.history?.percent ?: PROGRESS_NONE, animate = true)
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

	private fun onScrobblingInfoChanged(scrobblings: List<ScrobblingInfo>) {
		var adapter = binding.recyclerViewScrobbling.adapter as? ScrollingInfoAdapter
		binding.recyclerViewScrobbling.isGone = scrobblings.isEmpty()
		if (adapter != null) {
			adapter.items = scrobblings
		} else {
			adapter = ScrollingInfoAdapter(viewLifecycleOwner, coil, childFragmentManager)
			adapter.items = scrobblings
			binding.recyclerViewScrobbling.adapter = adapter
			binding.recyclerViewScrobbling.addItemDecoration(ScrobblingItemDecoration())
		}
	}

	override fun onClick(v: View) {
		val manga = viewModel.manga.value ?: return
		when (v.id) {
			R.id.textView_author -> {
				startActivity(
					SearchActivity.newIntent(
						context = v.context,
						source = manga.source,
						query = manga.author ?: return,
					),
				)
			}

			R.id.textView_source -> {
				startActivity(
					MangaListActivity.newIntent(
						context = v.context,
						source = manga.source,
					),
				)
			}

			R.id.imageView_cover -> {
				startActivity(
					ImageActivity.newIntent(
						v.context,
						manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl },
						manga.source,
					),
					scaleUpActivityOptionsOf(v).toBundle(),
				)
			}
		}
	}

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag ?: return
		startActivity(MangaListActivity.newIntent(requireContext(), setOf(tag)))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			bottom = (
				(activity as? NoModalBottomSheetOwner)?.bsHeader?.measureHeight()
					?.plus(insets.bottom)?.plus(resources.resolveDp(16))
				)
				?: insets.bottom,
		)
	}

	private fun bindTags(manga: Manga) {
		binding.chipsTags.setChips(
			manga.tags.map { tag ->
				ChipsView.ChipModel(
					title = tag.title,
					tint = tagHighlighter.getTint(tag),
					data = tag,
					isCheckable = false,
					isChecked = false,
				)
			},
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
			.size(CoverSizeResolver(binding.imageViewCover))
			.data(imageUrl)
			.tag(manga.source)
			.crossfade(requireContext())
			.lifecycle(viewLifecycleOwner)
			.placeholderMemoryCacheKey(manga.coverUrl)
		val previousDrawable = lastResult?.drawable
		if (previousDrawable != null) {
			request.fallback(previousDrawable)
				.placeholder(previousDrawable)
				.error(previousDrawable)
		} else {
			request.fallback(R.drawable.ic_placeholder)
				.placeholder(R.drawable.ic_placeholder)
				.error(R.drawable.ic_error_placeholder)
		}
		request.enqueueWith(coil)
	}
}
