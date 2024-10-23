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
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.lifecycle
import coil3.request.placeholder
import coil3.request.target
import coil3.request.transformations
import coil3.size.Scale
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.model.UnknownMangaSource
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.iconResId
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.titleResId
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.OnContextClickListenerCompat
import org.koitharu.kotatsu.core.ui.image.ChipIconTarget
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.BottomSheetCollapseCallback
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.crossfade
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.drawable
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.isTextTruncated
import org.koitharu.kotatsu.core.util.ext.joinToStringWithLimit
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.parentView
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.core.util.ext.setNavigationBarTransparentCompat
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ActivityDetailsBinding
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.data.ReadingTime
import org.koitharu.kotatsu.details.service.MangaPrefetchService
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet
import org.koitharu.kotatsu.details.ui.related.RelatedMangaActivity
import org.koitharu.kotatsu.details.ui.scrobbling.ScrobblingItemDecoration
import org.koitharu.kotatsu.details.ui.scrobbling.ScrollingInfoAdapter
import org.koitharu.kotatsu.download.ui.dialog.DownloadDialogFragment
import org.koitharu.kotatsu.download.ui.worker.DownloadStartedObserver
import org.koitharu.kotatsu.favourites.ui.categories.select.FavoriteSheet
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.mangaGridItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import org.koitharu.kotatsu.list.ui.size.StaticItemSizeResolver
import org.koitharu.kotatsu.local.ui.info.LocalInfoDialog
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.ellipsize
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.ui.selector.ScrobblingSelectorSheet
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.stats.ui.sheet.MangaStatsSheet
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class DetailsActivity :
	BaseActivity<ActivityDetailsBinding>(),
	View.OnClickListener,
	View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, View.OnLayoutChangeListener,
	ViewTreeObserver.OnDrawListener, ChipsView.OnChipClickListener, OnListItemClickListener<Bookmark>,
	OnContextClickListenerCompat {

	@Inject
	lateinit var shortcutManager: AppShortcutManager

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var listMapper: MangaListMapper

	private val viewModel: DetailsViewModel by viewModels()
	private lateinit var menuProvider: DetailsMenuProvider

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDetailsBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(false)
		}
		viewBinding.buttonRead.setOnClickListener(this)
		viewBinding.buttonRead.setOnLongClickListener(this)
		viewBinding.buttonRead.setOnContextClickListenerCompat(this)
		viewBinding.buttonDownload?.setOnClickListener(this)
		viewBinding.infoLayout.chipBranch.setOnClickListener(this)
		viewBinding.infoLayout.chipSize.setOnClickListener(this)
		viewBinding.infoLayout.chipSource.setOnClickListener(this)
		viewBinding.infoLayout.chipFavorite.setOnClickListener(this)
		viewBinding.infoLayout.chipAuthor.setOnClickListener(this)
		viewBinding.infoLayout.chipTime.setOnClickListener(this)
		viewBinding.imageViewCover.setOnClickListener(this)
		viewBinding.buttonDescriptionMore.setOnClickListener(this)
		viewBinding.buttonScrobblingMore.setOnClickListener(this)
		viewBinding.buttonRelatedMore.setOnClickListener(this)
		viewBinding.infoLayout.chipSource.setOnClickListener(this)
		viewBinding.infoLayout.chipSize.setOnClickListener(this)
		viewBinding.textViewDescription.addOnLayoutChangeListener(this)
		viewBinding.textViewDescription.viewTreeObserver.addOnDrawListener(this)
		viewBinding.textViewDescription.movementMethod = LinkMovementMethodCompat.getInstance()
		viewBinding.chipsTags.onChipClickListener = this
		TitleScrollCoordinator(viewBinding.textViewTitle).attach(viewBinding.scrollView)
		viewBinding.containerBottomSheet?.let { sheet ->
			onBackPressedDispatcher.addCallback(BottomSheetCollapseCallback(sheet))
		}
		TitleExpandListener(viewBinding.textViewTitle).attach()

		viewModel.mangaDetails.filterNotNull().observe(this, ::onMangaUpdated)
		viewModel.onMangaRemoved.observeEvent(this, ::onMangaRemoved)
		viewModel.onError
			.filterNot { ChaptersPagesSheet.isShown(supportFragmentManager) }
			.observeEvent(this, DetailsErrorObserver(this, viewModel, exceptionResolver))
		viewModel.onActionDone
			.filterNot { ChaptersPagesSheet.isShown(supportFragmentManager) }
			.observeEvent(this, ReversibleActionObserver(viewBinding.scrollView, null))
		combine(viewModel.historyInfo, viewModel.isLoading, ::Pair).observe(this) {
			onHistoryChanged(it.first, it.second)
		}
		viewModel.isLoading.observe(this, ::onLoadingStateChanged)
		viewModel.scrobblingInfo.observe(this, ::onScrobblingInfoChanged)
		viewModel.localSize.observe(this, ::onLocalSizeChanged)
		viewModel.relatedManga.observe(this, ::onRelatedMangaChanged)
		viewModel.readingTime.observe(this, ::onReadingTimeChanged)
		viewModel.selectedBranch.observe(this) {
			viewBinding.infoLayout.chipBranch.text = it.ifNullOrEmpty { getString(R.string.system_default) }
		}
		viewModel.favouriteCategories.observe(this, ::onFavoritesChanged)
		val menuInvalidator = MenuInvalidator(this)
		viewModel.isStatsAvailable.observe(this, menuInvalidator)
		viewModel.remoteManga.observe(this, menuInvalidator)
		viewModel.branches.observe(this) {
			viewBinding.infoLayout.chipBranch.isVisible = it.size > 1 || !it.firstOrNull()?.name.isNullOrEmpty()
			viewBinding.infoLayout.chipBranch.isCloseIconVisible = it.size > 1
		}
		viewModel.chapters.observe(this, PrefetchObserver(this))
		viewModel.onDownloadStarted
			.filterNot { ChaptersPagesSheet.isShown(supportFragmentManager) }
			.observeEvent(this, DownloadStartedObserver(viewBinding.scrollView))

		DownloadDialogFragment.registerCallback(this, viewBinding.scrollView)
		menuProvider = DetailsMenuProvider(
			activity = this,
			viewModel = viewModel,
			snackbarHost = viewBinding.scrollView,
			appShortcutManager = shortcutManager,
		)
		addMenuProvider(menuProvider)
	}

	override fun isNsfwContent(): Flow<Boolean> = viewModel.manga.map { it?.isNsfw == true }

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_read -> openReader(isIncognitoMode = false)
			R.id.chip_branch -> showBranchPopupMenu(v)
			R.id.button_download -> {
				val manga = viewModel.manga.value ?: return
				DownloadDialogFragment.show(supportFragmentManager, listOf(manga))
			}

			R.id.chip_author -> {
				val manga = viewModel.manga.value ?: return
				startActivity(
					MangaListActivity.newIntent(
						context = v.context,
						source = manga.source,
						filter = MangaListFilter(query = manga.author),
					),
				)
			}

			R.id.chip_source -> {
				val manga = viewModel.manga.value ?: return
				startActivity(
					MangaListActivity.newIntent(
						context = v.context,
						source = manga.source,
						filter = null,
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

			R.id.chip_time -> {
				if (viewModel.isStatsAvailable.value) {
					val manga = viewModel.manga.value ?: return
					MangaStatsSheet.show(supportFragmentManager, manga)
				} else {
					// TODO
				}
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

			R.id.button_related_more -> {
				val manga = viewModel.manga.value ?: return
				startActivity(RelatedMangaActivity.newIntent(v.context, manga))
			}
		}
	}

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag ?: return
		// TODO dialog
		startActivity(MangaListActivity.newIntent(this, tag.source, MangaListFilter(tags = setOf(tag))))
	}

	override fun onContextClick(v: View): Boolean = onLongClick(v)

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

			else -> false
		}
	}

	override fun onItemClick(item: Bookmark, view: View) {
		startActivity(
			ReaderActivity.IntentBuilder(view.context).bookmark(item).incognito(true).build(),
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

	private fun onFavoritesChanged(categories: Set<FavouriteCategory>) {
		val chip = viewBinding.infoLayout.chipFavorite
		chip.setChipIconResource(if (categories.isEmpty()) R.drawable.ic_heart_outline else R.drawable.ic_heart)
		chip.text = if (categories.isEmpty()) {
			getString(R.string.add_to_favourites)
		} else {
			categories.joinToStringWithLimit(this, FAV_LABEL_LIMIT) { it.title }
		}
	}

	private fun onReadingTimeChanged(time: ReadingTime?) {
		val chip = viewBinding.infoLayout.chipTime
		chip.textAndVisible = time?.formatShort(chip.resources)
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

	private fun onRelatedMangaChanged(related: List<MangaListModel>) {
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
					startActivity(newIntent(view.context, item))
				},
			).also { rv.adapter = it }
		adapter.items = related
		viewBinding.groupRelated.isVisible = true
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val button = viewBinding.buttonDownload ?: return
		if (isLoading) {
			button.setImageDrawable(
				CircularProgressDrawable(this).also {
					it.setStyle(CircularProgressDrawable.LARGE)
					it.setColorSchemeColors(getThemeColor(materialR.attr.colorControlNormal))
					it.start()
				},
			)
		} else {
			button.setImageResource(R.drawable.ic_download)
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
			// Main
			loadCover(manga)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitle
			infoLayout.chipAuthor.textAndVisible = manga.author?.ellipsize(AUTHOR_LABEL_LIMIT)
			if (manga.hasRating) {
				ratingBar.rating = manga.rating * ratingBar.numStars
				ratingBar.isVisible = true
			} else {
				ratingBar.isVisible = false
			}

			manga.state?.let { state ->
				textViewState.textAndVisible = resources.getString(state.titleResId)
				imageViewState.setImageResource(state.iconResId)
				imageViewState.isVisible = true
			} ?: run {
				textViewState.isVisible = false
				imageViewState.isVisible = false
			}

			if (manga.source == LocalMangaSource || manga.source == UnknownMangaSource) {
				infoLayout.chipSource.isVisible = false
			} else {
				infoLayout.chipSource.text = manga.source.getTitle(this@DetailsActivity)
				infoLayout.chipSource.isVisible = true
			}

			textViewNsfw.isVisible = manga.isNsfw

			// Chips
			bindTags(manga)

			textViewDescription.text = details.description.ifNullOrEmpty { getString(R.string.no_description) }

			viewBinding.infoLayout.chipSource.also { chip ->
				ImageRequest.Builder(this@DetailsActivity)
					.data(manga.source.faviconUri())
					.lifecycle(this@DetailsActivity)
					.crossfade(false)
					.size(resources.getDimensionPixelSize(materialR.dimen.m3_chip_icon_size))
					.target(ChipIconTarget(chip))
					.placeholder(R.drawable.ic_web)
					.fallback(R.drawable.ic_web)
					.error(R.drawable.ic_web)
					.mangaSourceExtra(manga.source)
					.transformations(RoundedCornersTransformation(resources.getDimension(R.dimen.chip_icon_corner)))
					.allowRgb565(true)
					.enqueueWith(coil)
			}

			title = manga.title
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
		viewBinding.cardChapters?.updateLayoutParams<MarginLayoutParams> {
			val baseOffset = leftMargin
			bottomMargin = insets.bottom + baseOffset
			topMargin = insets.bottom + baseOffset
		}
		viewBinding.scrollView.updatePadding(
			bottom = insets.bottom,
		)
		viewBinding.containerBottomSheet?.let { bs ->
			window.setNavigationBarTransparentCompat(this, bs.elevation, 0.9f)
		}
	}

	private fun onHistoryChanged(info: HistoryInfo, isLoading: Boolean) = with(viewBinding) {
		buttonRead.setTitle(if (info.canContinue) R.string._continue else R.string.read)
		buttonRead.subtitle = when {
			isLoading -> getString(R.string.loading_)
			info.isIncognitoMode -> getString(R.string.incognito_mode)
			info.isChapterMissing -> getString(R.string.chapter_is_missing)
			info.currentChapter >= 0 -> getString(R.string.chapter_d_of_d, info.currentChapter + 1, info.totalChapters)
			info.totalChapters == 0 -> getString(R.string.no_chapters)
			info.totalChapters == -1 -> getString(R.string.error_occurred)
			else -> resources.getQuantityString(R.plurals.chapters, info.totalChapters, info.totalChapters)
		}
		val isFirstCall = buttonRead.tag == null
		buttonRead.tag = Unit
		buttonRead.setProgress(info.percent.coerceIn(0f, 1f), !isFirstCall)
		buttonDownload?.isEnabled = info.isValid && info.canDownload
		buttonRead.isEnabled = info.isValid
	}

	private fun showBranchPopupMenu(v: View) {
		val branches = viewModel.branches.value
		if (branches.size <= 1) {
			return
		}
		val menu = PopupMenu(v.context, v)
		for ((i, branch) in branches.withIndex()) {
			val title = buildSpannedString {
				if (branch.isCurrent) {
					inSpans(
						ImageSpan(
							this@DetailsActivity,
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
			val item = menu.menu.add(R.id.group_branches, Menu.NONE, i, title)
			item.isCheckable = true
			item.isChecked = branch.isSelected
		}
		menu.menu.setGroupCheckable(R.id.group_branches, true, true)
		menu.setOnMenuItemClickListener {
			viewModel.setSelectedBranch(branches.getOrNull(it.order)?.name)
			true
		}
		menu.show()
	}

	private fun openReader(isIncognitoMode: Boolean) {
		val manga = viewModel.manga.value ?: return
		if (viewModel.historyInfo.value.isChapterMissing) {
			Snackbar.make(viewBinding.scrollView, R.string.chapter_is_missing, Snackbar.LENGTH_SHORT)
				.show()
		} else {
			startActivity(
				ReaderActivity.IntentBuilder(this)
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
		viewBinding.chipsTags.setChips(listMapper.mapTags(manga.tags))
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
			.scale(Scale.FILL)
			.data(imageUrl)
			.mangaSourceExtra(manga.source)
			.crossfade(this)
			.lifecycle(this)
			.placeholderMemoryCacheKey(manga.coverUrl)
		val previousDrawable = lastResult?.drawable
		if (previousDrawable != null) {
			request.fallback(previousDrawable)
				.placeholder(previousDrawable)
				.error(previousDrawable)
		} else {
			request.defaultPlaceholders(this)
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

		private const val FAV_LABEL_LIMIT = 16
		private const val AUTHOR_LABEL_LIMIT = 16

		fun newIntent(context: Context, manga: Manga): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(manga))
		}

		fun newIntent(context: Context, mangaId: Long): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_ID, mangaId)
		}
	}
}
