package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.adapter.BookmarksAdapter
import org.koitharu.kotatsu.bookmarks.ui.sheet.BookmarksSheet
import org.koitharu.kotatsu.core.model.countChaptersByBranch
import org.koitharu.kotatsu.core.model.iconResId
import org.koitharu.kotatsu.core.model.titleResId
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.crossfade
import org.koitharu.kotatsu.core.util.ext.drawableTop
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.isTextTruncated
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.parentView
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.FragmentDetailsBinding
import org.koitharu.kotatsu.details.data.ReadingTime
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.related.RelatedMangaActivity
import org.koitharu.kotatsu.details.ui.scrobbling.ScrobblingItemDecoration
import org.koitharu.kotatsu.details.ui.scrobbling.ScrollingInfoAdapter
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.mangaGridItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import org.koitharu.kotatsu.list.ui.size.StaticItemSizeResolver
import org.koitharu.kotatsu.local.ui.info.LocalInfoDialog
import org.koitharu.kotatsu.main.ui.owners.NoModalBottomSheetOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.ui.selector.ScrobblingSelectorSheet
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import javax.inject.Inject

@AndroidEntryPoint
class DetailsFragment :
	BaseFragment<FragmentDetailsBinding>(),
	View.OnClickListener,
	ChipsView.OnChipClickListener,
	OnListItemClickListener<Bookmark>, ViewTreeObserver.OnDrawListener, View.OnLayoutChangeListener {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var tagHighlighter: ListExtraProvider

	private val viewModel by activityViewModels<DetailsViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentDetailsBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentDetailsBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewAuthor.setOnClickListener(this)
		binding.imageViewCover.setOnClickListener(this)
		binding.buttonDescriptionMore.setOnClickListener(this)
		binding.buttonBookmarksMore.setOnClickListener(this)
		binding.buttonScrobblingMore.setOnClickListener(this)
		binding.buttonRelatedMore.setOnClickListener(this)
		binding.infoLayout.textViewSource.setOnClickListener(this)
		binding.infoLayout.textViewSize.setOnClickListener(this)
		binding.textViewDescription.addOnLayoutChangeListener(this)
		binding.textViewDescription.viewTreeObserver.addOnDrawListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethodCompat.getInstance()
		binding.chipsTags.onChipClickListener = this
		binding.recyclerViewRelated.addItemDecoration(
			SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing)),
		)
		TitleScrollCoordinator(binding.textViewTitle).attach(binding.scrollView)
		viewModel.manga.filterNotNull().observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.historyInfo.observe(viewLifecycleOwner, ::onHistoryChanged)
		viewModel.bookmarks.observe(viewLifecycleOwner, ::onBookmarksChanged)
		viewModel.scrobblingInfo.observe(viewLifecycleOwner, ::onScrobblingInfoChanged)
		viewModel.description.observe(viewLifecycleOwner, ::onDescriptionChanged)
		viewModel.localSize.observe(viewLifecycleOwner, ::onLocalSizeChanged)
		viewModel.relatedManga.observe(viewLifecycleOwner, ::onRelatedMangaChanged)
		viewModel.chapters.observe(viewLifecycleOwner, ::onChaptersChanged)
		viewModel.readingTime.observe(viewLifecycleOwner, ::onReadingTimeChanged)
	}

	override fun onItemClick(item: Bookmark, view: View) {
		startActivity(
			ReaderActivity.IntentBuilder(view.context).bookmark(item).incognito(true).build(),
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

	override fun onDraw() {
		viewBinding?.run {
			buttonDescriptionMore.isVisible = textViewDescription.maxLines == Int.MAX_VALUE ||
				textViewDescription.isTextTruncated
		}
	}

	override fun onLayoutChange(
		v: View?,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int,
		oldLeft: Int,
		oldTop: Int,
		oldRight: Int,
		oldBottom: Int
	) {
		with(viewBinding ?: return) {
			buttonDescriptionMore.isVisible = textViewDescription.isTextTruncated
		}
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

			infoLayout.textViewState.apply {
				manga.state?.let { state ->
					textAndVisible = resources.getString(state.titleResId)
					drawableTop = ContextCompat.getDrawable(context, state.iconResId)
				} ?: run {
					isVisible = false
				}
			}
			if (manga.source == MangaSource.LOCAL || manga.source == MangaSource.DUMMY) {
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
			val chaptersText = resources.getQuantityString(R.plurals.chapters, count, count)
			infoLayout.textViewChapters.text = chaptersText
		}
	}

	private fun onReadingTimeChanged(time: ReadingTime?) {
		val binding = viewBinding ?: return
		if (time == null) {
			binding.approximateReadTimeLayout.isVisible = false
			return
		}
		binding.approximateReadTime.text = time.format(resources)
		binding.approximateReadTimeTitle.setText(
			if (time.isContinue) R.string.approximate_remaining_time else R.string.approximate_reading_time,
		)
		binding.approximateReadTimeLayout.isVisible = true
	}

	private fun onDescriptionChanged(description: CharSequence?) {
		val tv = requireViewBinding().textViewDescription
		if (description.isNullOrBlank()) {
			tv.setText(R.string.no_description)
		} else {
			tv.text = description
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

	private fun onRelatedMangaChanged(related: List<MangaItemModel>) {
		if (related.isEmpty()) {
			requireViewBinding().groupRelated.isVisible = false
			return
		}
		val rv = viewBinding?.recyclerViewRelated ?: return

		@Suppress("UNCHECKED_CAST")
		val adapter = (rv.adapter as? BaseListAdapter<ListModel>) ?: BaseListAdapter<ListModel>()
			.addDelegate(
				ListItemType.MANGA_GRID,
				mangaGridItemAD(
					coil, viewLifecycleOwner,
					StaticItemSizeResolver(resources.getDimensionPixelSize(R.dimen.smaller_grid_width)),
				) { item, view ->
					startActivity(DetailsActivity.newIntent(view.context, item))
				},
			).also { rv.adapter = it }
		adapter.items = related
		requireViewBinding().groupRelated.isVisible = true
	}

	private fun onHistoryChanged(history: HistoryInfo) {
		requireViewBinding().progressView.setPercent(history.history?.percent ?: PROGRESS_NONE, animate = true)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		requireViewBinding().progressBar.showOrHide(isLoading)
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
		requireViewBinding().groupScrobbling.isGone = scrobblings.isEmpty()
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

			R.id.textView_size -> {
				LocalInfoDialog.show(parentFragmentManager, manga)
			}

			R.id.imageView_cover -> {
				startActivity(
					ImageActivity.newIntent(
						v.context,
						manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl },
						manga.source,
					),
					scaleUpActivityOptionsOf(v),
				)
			}

			R.id.button_description_more -> {
				val tv = requireViewBinding().textViewDescription
				TransitionManager.beginDelayedTransition(tv.parentView)
				if (tv.maxLines in 1 until Integer.MAX_VALUE) {
					tv.maxLines = Integer.MAX_VALUE
				} else {
					tv.maxLines = resources.getInteger(R.integer.details_description_lines)
				}
			}

			R.id.button_scrobbling_more -> {
				ScrobblingSelectorSheet.show(parentFragmentManager, manga, null)
			}

			R.id.button_bookmarks_more -> {
				BookmarksSheet.show(parentFragmentManager, manga)
			}

			R.id.button_related_more -> {
				startActivity(RelatedMangaActivity.newIntent(v.context, manga))
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
					tint = tagHighlighter.getTagTint(tag),
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
		if (lastResult is SuccessResult && lastResult.request.data == imageUrl) {
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
