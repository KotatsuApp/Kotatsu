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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import coil.ImageLoader
import coil.request.ImageRequest
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.adapter.BookmarksAdapter
import org.koitharu.kotatsu.core.model.countChaptersByBranch
import org.koitharu.kotatsu.core.parser.MangaTagHighlighter
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.crossfade
import org.koitharu.kotatsu.core.util.ext.drawableTop
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.FragmentDetailsBinding
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.scrobbling.ScrobblingItemDecoration
import org.koitharu.kotatsu.details.ui.scrobbling.ScrollingInfoAdapter
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
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

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentDetailsBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentDetailsBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewAuthor.setOnClickListener(this)
		binding.imageViewCover.setOnClickListener(this)
		binding.infoLayout.textViewSource.setOnClickListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethod.getInstance()
		binding.chipsTags.onChipClickListener = this
		TitleScrollCoordinator(binding.textViewTitle).attach(binding.scrollView)
		viewModel.manga.filterNotNull().observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.historyInfo.observe(viewLifecycleOwner, ::onHistoryChanged)
		viewModel.bookmarks.observe(viewLifecycleOwner, ::onBookmarksChanged)
		viewModel.scrobblingInfo.observe(viewLifecycleOwner, ::onScrobblingInfoChanged)
		viewModel.description.observe(viewLifecycleOwner, ::onDescriptionChanged)
		viewModel.chapters.observe(viewLifecycleOwner, ::onChaptersChanged)
		viewModel.localSize.observe(viewLifecycleOwner, ::onLocalSizeChanged)
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
		with(requireViewBinding()) {
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
			} else {
				infoLayout.textViewSource.text = manga.source.title
				infoLayout.textViewSource.isVisible = true
			}

			infoLayout.textViewNsfw.isVisible = manga.isNsfw

			// Chips
			bindTags(manga)
		}
	}

	private fun onChaptersChanged(chapters: List<ChapterListItem>?) {
		val infoLayout = requireViewBinding().infoLayout
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
			requireViewBinding().textViewDescription.setText(R.string.no_description)
		} else {
			requireViewBinding().textViewDescription.text = description
		}
	}

	private fun onLocalSizeChanged(size: Long) {
		val textView = requireViewBinding().infoLayout.textViewSize
		if (size == 0L) {
			textView.isVisible = false
		} else {
			textView.text = FileSize.BYTES.format(textView.context, size)
			textView.isVisible = true
		}
	}

	private fun onHistoryChanged(history: HistoryInfo) {
		requireViewBinding().progressView.setPercent(history.history?.percent ?: PROGRESS_NONE, animate = true)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		if (isLoading) {
			requireViewBinding().progressBar.show()
		} else {
			requireViewBinding().progressBar.hide()
		}
	}

	private fun onBookmarksChanged(bookmarks: List<Bookmark>) {
		var adapter = requireViewBinding().recyclerViewBookmarks.adapter as? BookmarksAdapter
		requireViewBinding().groupBookmarks.isGone = bookmarks.isEmpty()
		if (adapter != null) {
			adapter.items = bookmarks
		} else {
			adapter = BookmarksAdapter(coil, viewLifecycleOwner, this)
			adapter.items = bookmarks
			requireViewBinding().recyclerViewBookmarks.adapter = adapter
			val spacing = resources.getDimensionPixelOffset(R.dimen.bookmark_list_spacing)
			requireViewBinding().recyclerViewBookmarks.addItemDecoration(SpacingItemDecoration(spacing))
		}
	}

	private fun onScrobblingInfoChanged(scrobblings: List<ScrobblingInfo>) {
		var adapter = requireViewBinding().recyclerViewScrobbling.adapter as? ScrollingInfoAdapter
		requireViewBinding().recyclerViewScrobbling.isGone = scrobblings.isEmpty()
		if (adapter != null) {
			adapter.items = scrobblings
		} else {
			adapter = ScrollingInfoAdapter(viewLifecycleOwner, coil, childFragmentManager)
			adapter.items = scrobblings
			requireViewBinding().recyclerViewScrobbling.adapter = adapter
			requireViewBinding().recyclerViewScrobbling.addItemDecoration(ScrobblingItemDecoration())
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
		requireViewBinding().root.updatePadding(
			bottom = (
				(activity as? NoModalBottomSheetOwner)?.getBottomSheetCollapsedHeight()
					?.plus(insets.bottom)?.plus(resources.resolveDp(16))
				)
				?: insets.bottom,
		)
	}

	private fun bindTags(manga: Manga) {
		requireViewBinding().chipsTags.setChips(
			manga.tags.map { tag ->
				ChipsView.ChipModel(
					title = tag.title,
					tint = tagHighlighter.getTint(tag),
					icon = 0,
					data = tag,
					isCheckable = false,
					isChecked = false,
				)
			},
		)
	}

	private fun loadCover(manga: Manga) {
		val imageUrl = manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl }
		val lastResult = CoilUtils.result(requireViewBinding().imageViewCover)
		if (lastResult?.request?.data == imageUrl) {
			return
		}
		val request = ImageRequest.Builder(context ?: return)
			.target(requireViewBinding().imageViewCover)
			.size(CoverSizeResolver(requireViewBinding().imageViewCover))
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
