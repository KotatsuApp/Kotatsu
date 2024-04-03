package org.koitharu.kotatsu.details.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.style.DynamicDrawableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.adapter.BookmarksAdapter
import org.koitharu.kotatsu.bookmarks.ui.sheet.BookmarksSheet
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.iconResId
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.titleResId
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.image.ChipIconTarget
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ViewBadge
import org.koitharu.kotatsu.core.util.ext.crossfade
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.isTextTruncated
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.parentView
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ActivityDetailsNewBinding
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.data.ReadingTime
import org.koitharu.kotatsu.details.service.MangaPrefetchService
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet
import org.koitharu.kotatsu.details.ui.related.RelatedMangaActivity
import org.koitharu.kotatsu.details.ui.scrobbling.ScrobblingItemDecoration
import org.koitharu.kotatsu.details.ui.scrobbling.ScrollingInfoAdapter
import org.koitharu.kotatsu.download.ui.worker.DownloadStartedObserver
import org.koitharu.kotatsu.favourites.ui.categories.select.FavoriteSheet
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.mangaGridItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import org.koitharu.kotatsu.list.ui.size.StaticItemSizeResolver
import org.koitharu.kotatsu.local.ui.info.LocalInfoDialog
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.ellipsize
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.reader.ui.thumbnails.PagesThumbnailsSheet
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.ui.selector.ScrobblingSelectorSheet
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class DetailsActivity2 :
	BaseActivity<ActivityDetailsNewBinding>(),
	View.OnClickListener,
	View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, View.OnLayoutChangeListener,
	ViewTreeObserver.OnDrawListener, ChipsView.OnChipClickListener, OnListItemClickListener<Bookmark> {

	@Inject
	lateinit var shortcutManager: AppShortcutManager

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var tagHighlighter: ListExtraProvider

	private val viewModel: DetailsViewModel by viewModels()

	var bottomSheetMediator: ChaptersBottomSheetMediator? = null
		private set

	private lateinit var chaptersBadge: ViewBadge

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDetailsNewBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(false)
		}
		viewBinding.buttonRead.setOnClickListener(this)
		viewBinding.buttonRead.setOnLongClickListener(this)
		viewBinding.buttonRead.setOnContextClickListenerCompat(this)
		viewBinding.buttonChapters.setOnClickListener(this)
		viewBinding.infoLayout.chipBranch.setOnClickListener(this)
		viewBinding.infoLayout.chipSize.setOnClickListener(this)
		viewBinding.infoLayout.chipSource.setOnClickListener(this)
		viewBinding.infoLayout.chipFavorite.setOnClickListener(this)
		viewBinding.infoLayout.chipAuthor.setOnClickListener(this)
		viewBinding.imageViewCover.setOnClickListener(this)
		viewBinding.buttonDescriptionMore.setOnClickListener(this)
		viewBinding.buttonBookmarksMore.setOnClickListener(this)
		viewBinding.buttonScrobblingMore.setOnClickListener(this)
		viewBinding.buttonRelatedMore.setOnClickListener(this)
		viewBinding.infoLayout.chipSource.setOnClickListener(this)
		viewBinding.infoLayout.chipSize.setOnClickListener(this)
		viewBinding.textViewDescription.addOnLayoutChangeListener(this)
		viewBinding.textViewDescription.viewTreeObserver.addOnDrawListener(this)
		viewBinding.textViewDescription.movementMethod = LinkMovementMethodCompat.getInstance()
		viewBinding.chipsTags.onChipClickListener = this
		viewBinding.recyclerViewRelated.addItemDecoration(
			SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing)),
		)
		TitleScrollCoordinator(viewBinding.textViewTitle).attach(viewBinding.scrollView)

		chaptersBadge = ViewBadge(viewBinding.buttonChapters, this)

		viewModel.details.filterNotNull().observe(this, ::onMangaUpdated)
		viewModel.onMangaRemoved.observeEvent(this, ::onMangaRemoved)
		viewModel.newChaptersCount.observe(this, ::onNewChaptersChanged)
		viewModel.onError.observeEvent(
			this,
			SnackbarErrorObserver(viewBinding.scrollView, null, exceptionResolver) {
				if (it) viewModel.reload()
			},
		)
		viewModel.onActionDone.observeEvent(this, ReversibleActionObserver(viewBinding.scrollView, null))
		viewModel.historyInfo.observe(this, ::onHistoryChanged)
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.bookmarks.observe(this, ::onBookmarksChanged)
		viewModel.scrobblingInfo.observe(this, ::onScrobblingInfoChanged)
		viewModel.localSize.observe(this, ::onLocalSizeChanged)
		viewModel.relatedManga.observe(this, ::onRelatedMangaChanged)
		// viewModel.chapters.observe(this, ::onChaptersChanged)
		// viewModel.readingTime.observe(this, ::onReadingTimeChanged)
		viewModel.selectedBranch.observe(this) {
			viewBinding.infoLayout.chipBranch.text = it.ifNullOrEmpty { getString(R.string.system_default) }
		}
		viewModel.favouriteCategories.observe(this, ::onFavoritesChanged)
		val menuInvalidator = MenuInvalidator(this)
		viewModel.isStatsAvailable.observe(this, menuInvalidator)
		viewModel.remoteManga.observe(this, menuInvalidator)
		viewModel.branches.observe(this) {
			viewBinding.infoLayout.chipBranch.isVisible = it.size > 1
		}
		viewModel.chapters.observe(this, PrefetchObserver(this))
		viewModel.onDownloadStarted.observeEvent(
			this,
			DownloadStartedObserver(viewBinding.scrollView),
		)

		addMenuProvider(
			DetailsMenuProvider(
				activity = this,
				viewModel = viewModel,
				snackbarHost = viewBinding.scrollView,
				appShortcutManager = shortcutManager,
			),
		)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_read -> openReader(isIncognitoMode = false)
			R.id.chip_branch -> showBranchPopupMenu(v)
			R.id.button_chapters -> {
				ChaptersPagesSheet().showDistinct(supportFragmentManager, "ChaptersPagesSheet")
			}

			R.id.chip_author -> {
				val manga = viewModel.manga.value ?: return
				startActivity(
					SearchActivity.newIntent(
						context = v.context,
						source = manga.source,
						query = manga.author ?: return,
					),
				)
			}

			R.id.chip_source -> {
				val manga = viewModel.manga.value ?: return
				startActivity(
					MangaListActivity.newIntent(
						context = v.context,
						source = manga.source,
					),
				)
			}

			R.id.chip_size -> {
				val manga = viewModel.manga.value ?: return
				LocalInfoDialog.show(supportFragmentManager, manga)
			}

			R.id.chip_favorite -> {
				val manga = viewModel.manga.value ?: return
				FavoriteSheet.show(supportFragmentManager, manga)
			}

			R.id.imageView_cover -> {
				val manga = viewModel.manga.value ?: return
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
				val tv = viewBinding.textViewDescription
				TransitionManager.beginDelayedTransition(tv.parentView)
				if (tv.maxLines in 1 until Integer.MAX_VALUE) {
					tv.maxLines = Integer.MAX_VALUE
				} else {
					tv.maxLines = resources.getInteger(R.integer.details_description_lines)
				}
			}

			R.id.button_scrobbling_more -> {
				val manga = viewModel.manga.value ?: return
				ScrobblingSelectorSheet.show(supportFragmentManager, manga, null)
			}

			R.id.button_bookmarks_more -> {
				val manga = viewModel.manga.value ?: return
				BookmarksSheet.show(supportFragmentManager, manga)
			}

			R.id.button_related_more -> {
				val manga = viewModel.manga.value ?: return
				startActivity(RelatedMangaActivity.newIntent(v.context, manga))
			}
		}
	}

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag ?: return
		startActivity(MangaListActivity.newIntent(this, setOf(tag)))
	}

	override fun onLongClick(v: View): Boolean = when (v.id) {
		R.id.button_read -> {
			val menu = PopupMenu(v.context, v)
			menu.inflate(R.menu.popup_read)
			menu.menu.findItem(R.id.action_forget)?.isVisible = viewModel.historyInfo.value.run {
				!isIncognitoMode && history != null
			}
			menu.setOnMenuItemClickListener(this)
			menu.setForceShowIcon(true)
			menu.show()
			true
		}

		else -> false
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_incognito -> {
				openReader(isIncognitoMode = true)
				true
			}

			R.id.action_forget -> {
				viewModel.removeFromHistory()
				true
			}

			R.id.action_pages_thumbs -> {
				val history = viewModel.historyInfo.value.history
				PagesThumbnailsSheet.show(
					fm = supportFragmentManager,
					manga = viewModel.manga.value ?: return false,
					chapterId = history?.chapterId
						?: viewModel.chapters.value.firstOrNull()?.chapter?.id
						?: return false,
					currentPage = history?.page ?: 0,
				)
				true
			}

			else -> false
		}
	}

	override fun onItemClick(item: Bookmark, view: View) {
		startActivity(
			IntentBuilder(view.context).bookmark(item).incognito(true).build(),
		)
		Toast.makeText(view.context, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
	}

	override fun onDraw() {
		viewBinding.run {
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
		with(viewBinding) {
			buttonDescriptionMore.isVisible = textViewDescription.isTextTruncated
		}
	}

	private fun onChaptersChanged(chapters: List<ChapterListItem>?) {
		// TODO
	}

	private fun onFavoritesChanged(categories: Set<FavouriteCategory>) {
		val chip = viewBinding.infoLayout.chipFavorite
		chip.setChipIconResource(if (categories.isEmpty()) R.drawable.ic_heart_outline else R.drawable.ic_heart)
		chip.text = if (categories.isEmpty()) {
			getString(R.string.add_to_favourites)
		} else {
			if (categories.size == 1) {
				categories.first().title.ellipsize(FAV_LABEL_LIMIT)
			}
			buildString(FAV_LABEL_LIMIT + 6) {
				for ((i, cat) in categories.withIndex()) {
					if (i == 0) {
						append(cat.title.ellipsize(FAV_LABEL_LIMIT - 4))
					} else if (length + cat.title.length > FAV_LABEL_LIMIT) {
						append(", ")
						append(getString(R.string.list_ellipsize_pattern, categories.size - i))
						break
					} else {
						append(", ")
						append(cat.title)
					}
				}
			}
		}
	}

	private fun onReadingTimeChanged(time: ReadingTime?) {
		// TODO
	}

	private fun onDescriptionChanged(description: CharSequence?) {
		val tv = viewBinding.textViewDescription
		if (description.isNullOrBlank()) {
			tv.setText(R.string.no_description)
		} else {
			tv.text = description
		}
	}

	private fun onLocalSizeChanged(size: Long) {
		val chip = viewBinding.infoLayout.chipSize
		if (size == 0L) {
			chip.isVisible = false
		} else {
			chip.text = FileSize.BYTES.format(chip.context, size)
			chip.isVisible = true
		}
	}

	private fun onRelatedMangaChanged(related: List<MangaItemModel>) {
		if (related.isEmpty()) {
			viewBinding.groupRelated.isVisible = false
			return
		}
		val rv = viewBinding.recyclerViewRelated

		@Suppress("UNCHECKED_CAST")
		val adapter = (rv.adapter as? BaseListAdapter<ListModel>) ?: BaseListAdapter<ListModel>()
			.addDelegate(
				ListItemType.MANGA_GRID,
				mangaGridItemAD(
					coil, this,
					StaticItemSizeResolver(resources.getDimensionPixelSize(R.dimen.smaller_grid_width)),
				) { item, view ->
					startActivity(DetailsActivity.newIntent(view.context, item))
				},
			).also { rv.adapter = it }
		adapter.items = related
		viewBinding.groupRelated.isVisible = true
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val button = viewBinding.buttonChapters
		if (isLoading) {
			button.setImageDrawable(
				CircularProgressDrawable(this).also {
					it.setStyle(CircularProgressDrawable.LARGE)
					it.setColorSchemeColors(getThemeColor(materialR.attr.colorControlNormal))
					it.start()
				},
			)
		} else {
			button.setImageResource(R.drawable.ic_list_sheet)
		}
	}

	private fun onBookmarksChanged(bookmarks: List<Bookmark>) {
		var adapter = viewBinding.recyclerViewBookmarks.adapter as? BookmarksAdapter
		viewBinding.groupBookmarks.isGone = bookmarks.isEmpty()
		if (adapter != null) {
			adapter.items = bookmarks
		} else {
			adapter = BookmarksAdapter(coil, this, this)
			adapter.items = bookmarks
			viewBinding.recyclerViewBookmarks.adapter = adapter
			val spacing = resources.getDimensionPixelOffset(R.dimen.bookmark_list_spacing)
			viewBinding.recyclerViewBookmarks.addItemDecoration(SpacingItemDecoration(spacing))
		}
	}

	private fun onScrobblingInfoChanged(scrobblings: List<ScrobblingInfo>) {
		var adapter = viewBinding.recyclerViewScrobbling.adapter as? ScrollingInfoAdapter
		viewBinding.groupScrobbling.isGone = scrobblings.isEmpty()
		if (adapter != null) {
			adapter.items = scrobblings
		} else {
			adapter = ScrollingInfoAdapter(this, coil, supportFragmentManager)
			adapter.items = scrobblings
			viewBinding.recyclerViewScrobbling.adapter = adapter
			viewBinding.recyclerViewScrobbling.addItemDecoration(ScrobblingItemDecoration())
		}
	}

	private fun onMangaUpdated(details: MangaDetails) {
		with(viewBinding) {
			val manga = details.toManga()
			val hasChapters = !manga.chapters.isNullOrEmpty()
			// Main
			loadCover(manga)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitle
			infoLayout.chipAuthor.textAndVisible = manga.author
			if (manga.hasRating) {
				ratingBar.rating = manga.rating * ratingBar.numStars
				ratingBar.isVisible = true
			} else {
				ratingBar.isVisible = false
			}

			textViewState.apply {
				manga.state?.let { state ->
					textAndVisible = resources.getString(state.titleResId)
					drawableStart = ContextCompat.getDrawable(context, state.iconResId)
				} ?: run {
					isVisible = false
				}
			}
			if (manga.source == MangaSource.LOCAL || manga.source == MangaSource.DUMMY) {
				infoLayout.chipSource.isVisible = false
			} else {
				infoLayout.chipSource.text = manga.source.title
				infoLayout.chipSource.isVisible = true
			}

			textViewNsfw.isVisible = manga.isNsfw

			// Chips
			bindTags(manga)

			textViewDescription.text = details.description.ifNullOrEmpty { getString(R.string.no_description) }

			viewBinding.infoLayout.chipSource.also { chip ->
				ImageRequest.Builder(this@DetailsActivity2)
					.data(manga.source.faviconUri())
					.lifecycle(this@DetailsActivity2)
					.crossfade(false)
					.size(resources.getDimensionPixelSize(materialR.dimen.m3_chip_icon_size))
					.target(ChipIconTarget(chip))
					.placeholder(R.drawable.ic_web)
					.fallback(R.drawable.ic_web)
					.error(R.drawable.ic_web)
					.source(manga.source)
					.transformations(CircleCropTransformation())
					.allowRgb565(true)
					.enqueueWith(coil)
			}

			buttonChapters.isEnabled = hasChapters
			title = manga.title
			buttonRead.isEnabled = hasChapters
			invalidateOptionsMenu()
		}
	}

	private fun onMangaRemoved(manga: Manga) {
		Toast.makeText(
			this,
			getString(R.string._s_deleted_from_local_storage, manga.title),
			Toast.LENGTH_SHORT,
		).show()
		finishAfterTransition()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	private fun onHistoryChanged(info: HistoryInfo) {
		with(viewBinding.buttonRead) {
			if (info.history != null) {
				setTitle(R.string._continue)
			} else {
				setTitle(R.string.read)
			}
		}
		viewBinding.buttonRead.subtitle = when {
			!info.isValid -> getString(R.string.loading_)
			info.currentChapter >= 0 -> getString(
				R.string.chapter_d_of_d,
				info.currentChapter + 1,
				info.totalChapters,
			)

			info.totalChapters == 0 -> getString(R.string.no_chapters)
			else -> resources.getQuantityString(
				R.plurals.chapters,
				info.totalChapters,
				info.totalChapters,
			)
		}
		viewBinding.buttonRead.setProgress(info.history?.percent?.coerceIn(0f, 1f) ?: 0f, true)
	}

	private fun onNewChaptersChanged(count: Int) {
		chaptersBadge.counter = count
	}

	private fun showBranchPopupMenu(v: View) {
		val menu = PopupMenu(v.context, v)
		val branches = viewModel.branches.value
		for ((i, branch) in branches.withIndex()) {
			val title = buildSpannedString {
				if (branch.isCurrent) {
					inSpans(
						ImageSpan(
							this@DetailsActivity2,
							R.drawable.ic_current_chapter,
							DynamicDrawableSpan.ALIGN_BASELINE,
						),
					) {
						append(' ')
					}
					append(' ')
				}
				append(branch.name ?: getString(R.string.system_default))
				append(' ')
				append(' ')
				inSpans(
					ForegroundColorSpan(
						v.context.getThemeColor(
							android.R.attr.textColorSecondary,
							Color.LTGRAY,
						),
					),
					RelativeSizeSpan(0.74f),
				) {
					append(branch.count.toString())
				}
			}
			menu.menu.add(Menu.NONE, Menu.NONE, i, title)
		}
		menu.setOnMenuItemClickListener {
			viewModel.setSelectedBranch(branches.getOrNull(it.order)?.name)
			true
		}
		menu.show()
	}

	private fun openReader(isIncognitoMode: Boolean) {
		val manga = viewModel.manga.value ?: return
		val chapterId = viewModel.historyInfo.value.history?.chapterId
		if (chapterId != null && manga.chapters?.none { x -> x.id == chapterId } == true) {
			Snackbar.make(viewBinding.scrollView, R.string.chapter_is_missing, Snackbar.LENGTH_SHORT)
				.show()
		} else {
			startActivity(
				IntentBuilder(this)
					.manga(manga)
					.branch(viewModel.selectedBranchValue)
					.incognito(isIncognitoMode)
					.build(),
			)
			if (isIncognitoMode) {
				Toast.makeText(this, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun bindTags(manga: Manga) {
		viewBinding.chipsTags.isVisible = manga.tags.isNotEmpty()
		viewBinding.chipsTags.setChips(
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
		val lastResult = CoilUtils.result(viewBinding.imageViewCover)
		if (lastResult is SuccessResult && lastResult.request.data == imageUrl) {
			return
		}
		val request = ImageRequest.Builder(this)
			.target(viewBinding.imageViewCover)
			.size(CoverSizeResolver(viewBinding.imageViewCover))
			.data(imageUrl)
			.tag(manga.source)
			.crossfade(this)
			.lifecycle(this)
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

	private class PrefetchObserver(
		private val context: Context,
	) : FlowCollector<List<ChapterListItem>?> {

		private var isCalled = false

		override suspend fun emit(value: List<ChapterListItem>?) {
			if (value.isNullOrEmpty()) {
				return
			}
			if (!isCalled) {
				isCalled = true
				val item = value.find { it.isCurrent } ?: value.first()
				MangaPrefetchService.prefetchPages(context, item.chapter)
			}
		}
	}

	companion object {

		private const val FAV_LABEL_LIMIT = 10

		fun newIntent(context: Context, manga: Manga): Intent {
			return Intent(context, DetailsActivity2::class.java)
				.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(manga))
		}

		fun newIntent(context: Context, mangaId: Long): Intent {
			return Intent(context, DetailsActivity2::class.java)
				.putExtra(MangaIntent.KEY_ID, mangaId)
		}
	}
}
