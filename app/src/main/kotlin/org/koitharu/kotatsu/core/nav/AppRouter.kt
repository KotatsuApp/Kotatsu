package org.koitharu.kotatsu.core.nav

import android.accounts.Account
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.UiContext
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.findFragment
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.EntryPointAccessors
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.alternatives.ui.AlternativesActivity
import org.koitharu.kotatsu.backups.ui.backup.BackupDialogFragment
import org.koitharu.kotatsu.backups.ui.restore.RestoreDialogFragment
import org.koitharu.kotatsu.bookmarks.ui.AllBookmarksActivity
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareActivity
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.image.CoilMemoryCacheKey
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.MangaSourceInfo
import org.koitharu.kotatsu.core.model.appUrl
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.isBroken
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaListFilter
import org.koitharu.kotatsu.core.model.parcelable.ParcelableMangaPage
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.external.ExternalMangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.prefs.TriStateOption
import org.koitharu.kotatsu.core.ui.dialog.BigButtonsAlertDialog
import org.koitharu.kotatsu.core.ui.dialog.ErrorDetailsDialog
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.core.util.ext.connectivityManager
import org.koitharu.kotatsu.core.util.ext.findActivity
import org.koitharu.kotatsu.core.util.ext.getThemeDrawable
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet
import org.koitharu.kotatsu.details.ui.related.RelatedMangaActivity
import org.koitharu.kotatsu.details.ui.scrobbling.ScrobblingInfoSheet
import org.koitharu.kotatsu.download.ui.dialog.DownloadDialogFragment
import org.koitharu.kotatsu.download.ui.list.DownloadsActivity
import org.koitharu.kotatsu.favourites.ui.FavouritesActivity
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.favourites.ui.categories.select.FavoriteDialog
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.filter.ui.sheet.FilterSheetFragment
import org.koitharu.kotatsu.filter.ui.tags.TagsCatalogSheet
import org.koitharu.kotatsu.history.ui.HistoryActivity
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.list.ui.config.ListConfigBottomSheet
import org.koitharu.kotatsu.list.ui.config.ListConfigSection
import org.koitharu.kotatsu.local.ui.ImportDialogFragment
import org.koitharu.kotatsu.local.ui.info.LocalInfoDialog
import org.koitharu.kotatsu.main.ui.MainActivity
import org.koitharu.kotatsu.main.ui.welcome.WelcomeSheet
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.ellipsize
import org.koitharu.kotatsu.parsers.util.isNullOrEmpty
import org.koitharu.kotatsu.parsers.util.mapToArray
import org.koitharu.kotatsu.reader.ui.colorfilter.ColorFilterConfigActivity
import org.koitharu.kotatsu.reader.ui.config.ReaderConfigSheet
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.ui.config.ScrobblerConfigActivity
import org.koitharu.kotatsu.scrobbling.common.ui.selector.ScrobblingSelectorSheet
import org.koitharu.kotatsu.search.domain.SearchKind
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.multi.SearchActivity
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.about.AppUpdateActivity
import org.koitharu.kotatsu.settings.override.OverrideConfigActivity
import org.koitharu.kotatsu.settings.reader.ReaderTapGridConfigActivity
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import org.koitharu.kotatsu.settings.sources.catalog.SourcesCatalogActivity
import org.koitharu.kotatsu.settings.storage.MangaDirectorySelectDialog
import org.koitharu.kotatsu.settings.storage.directories.MangaDirectoriesActivity
import org.koitharu.kotatsu.settings.tracker.categories.TrackerCategoriesConfigSheet
import org.koitharu.kotatsu.stats.ui.StatsActivity
import org.koitharu.kotatsu.stats.ui.sheet.MangaStatsSheet
import org.koitharu.kotatsu.suggestions.ui.SuggestionsActivity
import org.koitharu.kotatsu.tracker.ui.updates.UpdatesActivity
import java.io.File
import androidx.appcompat.R as appcompatR

class AppRouter private constructor(
	private val activity: FragmentActivity?,
	private val fragment: Fragment?,
) {

	constructor(activity: FragmentActivity) : this(activity, null)

	constructor(fragment: Fragment) : this(null, fragment)

	private val settings: AppSettings by lazy {
		EntryPointAccessors.fromApplication<AppRouterEntryPoint>(checkNotNull(contextOrNull())).settings
	}

	/** Activities **/

	fun openList(source: MangaSource, filter: MangaListFilter?, sortOrder: SortOrder?) {
		startActivity(listIntent(contextOrNull() ?: return, source, filter, sortOrder))
	}

	fun openList(tag: MangaTag) = openList(tag.source, MangaListFilter(tags = setOf(tag)), null)

	fun openSearch(query: String, kind: SearchKind = SearchKind.SIMPLE) {
		startActivity(
			Intent(contextOrNull() ?: return, SearchActivity::class.java)
				.putExtra(KEY_QUERY, query)
				.putExtra(KEY_KIND, kind),
		)
	}

	fun openSearch(source: MangaSource, query: String) = openList(source, MangaListFilter(query = query), null)

	fun openDetails(manga: Manga) {
		startActivity(detailsIntent(contextOrNull() ?: return, manga))
	}

	fun openDetails(mangaId: Long) {
		startActivity(detailsIntent(contextOrNull() ?: return, mangaId))
	}

	fun openDetails(link: Uri) {
		startActivity(
			Intent(contextOrNull() ?: return, DetailsActivity::class.java)
				.setData(link),
		)
	}

	fun openReader(manga: Manga, anchor: View? = null) {
		openReader(
			ReaderIntent.Builder(contextOrNull() ?: return)
				.manga(manga)
				.build(),
			anchor,
		)
	}

	fun openReader(intent: ReaderIntent, anchor: View? = null) {
		val activityIntent = intent.intent
		if (settings.isReaderMultiTaskEnabled && activityIntent.data != null) {
			activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
		}
		startActivity(activityIntent, anchor?.let { view -> scaleUpActivityOptionsOf(view) })
	}

	fun openAlternatives(manga: Manga) {
		startActivity(
			Intent(contextOrNull() ?: return, AlternativesActivity::class.java)
				.putExtra(KEY_MANGA, ParcelableManga(manga)),
		)
	}

	fun openRelated(manga: Manga) {
		startActivity(
			Intent(contextOrNull(), RelatedMangaActivity::class.java)
				.putExtra(KEY_MANGA, ParcelableManga(manga)),
		)
	}

	fun openImage(url: String, source: MangaSource?, anchor: View? = null, preview: CoilMemoryCacheKey? = null) {
		startActivity(
			Intent(contextOrNull(), ImageActivity::class.java)
				.setData(url.toUri())
				.putExtra(KEY_SOURCE, source?.name)
				.putExtra(KEY_PREVIEW, preview),
			anchor?.let { scaleUpActivityOptionsOf(it) },
		)
	}

	fun openBookmarks() = startActivity(AllBookmarksActivity::class.java)

	fun openAppUpdate() = startActivity(AppUpdateActivity::class.java)

	fun openSuggestions() {
		startActivity(suggestionsIntent(contextOrNull() ?: return))
	}

	fun openSourcesCatalog() = startActivity(SourcesCatalogActivity::class.java)

	fun openDownloads() = startActivity(DownloadsActivity::class.java)

	fun openDirectoriesSettings() = startActivity(MangaDirectoriesActivity::class.java)

	fun openBrowser(url: String, source: MangaSource?, title: String?) {
		startActivity(browserIntent(contextOrNull() ?: return, url, source, title))
	}

	fun openColorFilterConfig(manga: Manga, page: MangaPage) {
		startActivity(
			Intent(contextOrNull(), ColorFilterConfigActivity::class.java)
				.putExtra(KEY_MANGA, ParcelableManga(manga))
				.putExtra(KEY_PAGES, ParcelableMangaPage(page)),
		)
	}

	fun openHistory() = startActivity(HistoryActivity::class.java)

	fun openFavorites() = startActivity(FavouritesActivity::class.java)

	fun openFavorites(category: FavouriteCategory) {
		startActivity(
			Intent(contextOrNull() ?: return, FavouritesActivity::class.java)
				.putExtra(KEY_ID, category.id)
				.putExtra(KEY_TITLE, category.title),
		)
	}

	fun openFavoriteCategories() = startActivity(FavouriteCategoriesActivity::class.java)

	fun openFavoriteCategoryEdit(categoryId: Long) {
		startActivity(
			Intent(contextOrNull() ?: return, FavouritesCategoryEditActivity::class.java)
				.putExtra(KEY_ID, categoryId),
		)
	}

	fun openFavoriteCategoryCreate() = openFavoriteCategoryEdit(FavouritesCategoryEditActivity.NO_ID)

	fun openMangaUpdates() {
		startActivity(mangaUpdatesIntent(contextOrNull() ?: return))
	}

	fun openMangaOverrideConfig(manga: Manga) {
		val intent = overrideEditIntent(contextOrNull() ?: return, manga)
		startActivity(intent)
	}

	fun openSettings() = startActivity(SettingsActivity::class.java)

	fun openReaderSettings() {
		startActivity(readerSettingsIntent(contextOrNull() ?: return))
	}

	fun openProxySettings() {
		startActivity(proxySettingsIntent(contextOrNull() ?: return))
	}

	fun openDownloadsSetting() {
		startActivity(downloadsSettingsIntent(contextOrNull() ?: return))
	}

	fun openSourceSettings(source: MangaSource) {
		startActivity(sourceSettingsIntent(contextOrNull() ?: return, source))
	}

	fun openSuggestionsSettings() {
		startActivity(suggestionsSettingsIntent(contextOrNull() ?: return))
	}

	fun openSourcesSettings() {
		startActivity(sourcesSettingsIntent(contextOrNull() ?: return))
	}

	fun openDiscordSettings() {
		startActivity(discordSettingsIntent(contextOrNull() ?: return))
	}

	fun openReaderTapGridSettings() = startActivity(ReaderTapGridConfigActivity::class.java)

	fun openScrobblerSettings(scrobbler: ScrobblerService) {
		startActivity(
			Intent(contextOrNull() ?: return, ScrobblerConfigActivity::class.java)
				.putExtra(KEY_ID, scrobbler.id),
		)
	}

	fun openSourceAuth(source: MangaSource) {
		startActivity(sourceAuthIntent(contextOrNull() ?: return, source))
	}

	fun openManageSources() {
		startActivity(
			manageSourcesIntent(contextOrNull() ?: return),
		)
	}

	fun openStatistic() = startActivity(StatsActivity::class.java)

	@CheckResult
	fun openExternalBrowser(url: String, chooserTitle: CharSequence? = null): Boolean {
		val intent = Intent(Intent.ACTION_VIEW)
		intent.data = url.toUriOrNull() ?: return false
		return startActivitySafe(
			if (!chooserTitle.isNullOrEmpty()) {
				Intent.createChooser(intent, chooserTitle)
			} else {
				intent
			},
		)
	}

	@CheckResult
	fun openSystemSyncSettings(account: Account): Boolean {
		val args = Bundle(1)
		args.putParcelable(ACCOUNT_KEY, account)
		val intent = Intent(ACTION_ACCOUNT_SYNC_SETTINGS)
		intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
		return startActivitySafe(intent)
	}

	/** Dialogs **/

	fun showDownloadDialog(manga: Manga, snackbarHost: View?) = showDownloadDialog(setOf(manga), snackbarHost)

	fun showDownloadDialog(manga: Collection<Manga>, snackbarHost: View?) {
		if (manga.isEmpty()) {
			return
		}
		val fm = getFragmentManager() ?: return
		if (snackbarHost != null) {
			getLifecycleOwner()?.let { lifecycleOwner ->
				DownloadDialogFragment.registerCallback(fm, lifecycleOwner, snackbarHost)
			}
		} else {
			DownloadDialogFragment.unregisterCallback(fm)
		}
		DownloadDialogFragment().withArgs(1) {
			putParcelableArray(KEY_MANGA, manga.mapToArray { ParcelableManga(it, withDescription = false) })
		}.showDistinct()
	}

	fun showLocalInfoDialog(manga: Manga) {
		LocalInfoDialog().withArgs(1) {
			putParcelable(KEY_MANGA, ParcelableManga(manga))
		}.showDistinct()
	}

	fun showDirectorySelectDialog() {
		MangaDirectorySelectDialog().showDistinct()
	}

	fun showFavoriteDialog(manga: Manga) = showFavoriteDialog(setOf(manga))

	fun showFavoriteDialog(manga: Collection<Manga>) {
		if (manga.isEmpty()) {
			return
		}
		FavoriteDialog().withArgs(1) {
			putParcelableArrayList(
				KEY_MANGA_LIST,
				manga.mapTo(ArrayList(manga.size)) { ParcelableManga(it, withDescription = false) },
			)
		}.showDistinct()
	}

	fun showTagDialog(tag: MangaTag) {
		buildAlertDialog(contextOrNull() ?: return) {
			setIcon(R.drawable.ic_tag)
			setTitle(tag.title)
			setItems(
				arrayOf(
					context.getString(R.string.search_on_s, tag.source.getTitle(context)),
					context.getString(R.string.search_everywhere),
				),
			) { _, which ->
				when (which) {
					0 -> openList(tag)
					1 -> openSearch(tag.title, SearchKind.TAG)
				}
			}
			setNegativeButton(R.string.close, null)
			setCancelable(true)
		}.show()
	}

	fun showAuthorDialog(author: String, source: MangaSource) {
		buildAlertDialog(contextOrNull() ?: return) {
			setIcon(R.drawable.ic_user)
			setTitle(author)
			setItems(
				arrayOf(
					context.getString(R.string.search_on_s, source.getTitle(context)),
					context.getString(R.string.search_everywhere),
				),
			) { _, which ->
				when (which) {
					0 -> openList(source, MangaListFilter(author = author), null)
					1 -> openSearch(author, SearchKind.AUTHOR)
				}
			}
			setNegativeButton(R.string.close, null)
			setCancelable(true)
		}.show()
	}

	fun showShareDialog(manga: Manga) {
		if (manga.isBroken) {
			return
		}
		if (manga.isLocal) {
			manga.url.toUri().toFileOrNull()?.let {
				shareFile(it)
			}
			return
		}
		buildAlertDialog(contextOrNull() ?: return) {
			setIcon(context.getThemeDrawable(appcompatR.attr.actionModeShareDrawable))
			setTitle(R.string.share)
			setItems(
				arrayOf(
					context.getString(R.string.link_to_manga_in_app),
					context.getString(R.string.link_to_manga_on_s, manga.source.getTitle(context)),
				),
			) { _, which ->
				val link = when (which) {
					0 -> manga.appUrl.toString()
					1 -> manga.publicUrl
					else -> return@setItems
				}
				shareLink(link, manga.title)
			}
			setNegativeButton(android.R.string.cancel, null)
			setCancelable(true)
		}.show()
	}

	fun showErrorDialog(error: Throwable, url: String? = null) {
		ErrorDetailsDialog().withArgs(2) {
			putSerializable(KEY_ERROR, error)
			putString(KEY_URL, url)
		}.show()
	}

	fun showBackupRestoreDialog(fileUri: Uri) {
		RestoreDialogFragment().withArgs(1) {
			putString(KEY_FILE, fileUri.toString())
		}.show()
	}

	fun createBackup(destination: Uri) {
		BackupDialogFragment().withArgs(1) {
			putParcelable(KEY_DATA, destination)
		}.showDistinct()
	}

	fun showImportDialog() {
		ImportDialogFragment().showDistinct()
	}

	fun showFilterSheet(): Boolean = if (isFilterSupported()) {
		FilterSheetFragment().showDistinct()
	} else {
		false
	}

	fun showTagsCatalogSheet(excludeMode: Boolean) {
		if (!isFilterSupported()) {
			return
		}
		TagsCatalogSheet().withArgs(1) {
			putBoolean(KEY_EXCLUDE, excludeMode)
		}.showDistinct()
	}

	fun showListConfigSheet(section: ListConfigSection) {
		ListConfigBottomSheet().withArgs(1) {
			putParcelable(KEY_LIST_SECTION, section)
		}.showDistinct()
	}

	fun showStatisticSheet(manga: Manga) {
		MangaStatsSheet().withArgs(1) {
			putParcelable(KEY_MANGA, ParcelableManga(manga))
		}.showDistinct()
	}

	fun showReaderConfigSheet(mode: ReaderMode) {
		ReaderConfigSheet().withArgs(1) {
			putInt(KEY_READER_MODE, mode.id)
		}.showDistinct()
	}

	fun showWelcomeSheet() {
		WelcomeSheet().showDistinct()
	}

	fun showChapterPagesSheet() {
		ChaptersPagesSheet().showDistinct()
	}

	fun showChapterPagesSheet(defaultTab: Int) {
		ChaptersPagesSheet().withArgs(1) {
			putInt(KEY_TAB, defaultTab)
		}.showDistinct()
	}

	fun showScrobblingSelectorSheet(manga: Manga, scrobblerService: ScrobblerService?) {
		ScrobblingSelectorSheet().withArgs(2) {
			putParcelable(KEY_MANGA, ParcelableManga(manga))
			if (scrobblerService != null) {
				putInt(KEY_ID, scrobblerService.id)
			}
		}.show()
	}

	fun showScrobblingInfoSheet(index: Int) {
		ScrobblingInfoSheet().withArgs(1) {
			putInt(KEY_INDEX, index)
		}.showDistinct()
	}

	fun showTrackerCategoriesConfigSheet() {
		TrackerCategoriesConfigSheet().showDistinct()
	}

	fun askForDownloadOverMeteredNetwork(onConfirmed: (allow: Boolean) -> Unit) {
		val context = contextOrNull() ?: return
		when (settings.allowDownloadOnMeteredNetwork) {
			TriStateOption.ENABLED -> onConfirmed(true)
			TriStateOption.DISABLED -> onConfirmed(false)
			TriStateOption.ASK -> {
				if (!context.connectivityManager.isActiveNetworkMetered) {
					onConfirmed(true)
					return
				}
				val listener = DialogInterface.OnClickListener { _, which ->
					when (which) {
						DialogInterface.BUTTON_POSITIVE -> {
							settings.allowDownloadOnMeteredNetwork = TriStateOption.ENABLED
							onConfirmed(true)
						}

						DialogInterface.BUTTON_NEUTRAL -> {
							onConfirmed(true)
						}

						DialogInterface.BUTTON_NEGATIVE -> {
							settings.allowDownloadOnMeteredNetwork = TriStateOption.DISABLED
							onConfirmed(false)
						}
					}
				}
				BigButtonsAlertDialog.Builder(context)
					.setIcon(R.drawable.ic_network_cellular)
					.setTitle(R.string.download_cellular_confirm)
					.setPositiveButton(R.string.allow_always, listener)
					.setNeutralButton(R.string.allow_once, listener)
					.setNegativeButton(R.string.dont_allow, listener)
					.create()
					.show()
			}
		}
	}

	/** Public utils **/

	fun isFilterSupported(): Boolean = when {
		fragment != null -> FilterCoordinator.find(fragment) != null
		activity != null -> activity is FilterCoordinator.Owner
		else -> false
	}

	fun isChapterPagesSheetShown(): Boolean {
		val sheet = getFragmentManager()?.findFragmentByTag(fragmentTag<ChaptersPagesSheet>()) as? ChaptersPagesSheet
		return sheet?.dialog?.isShowing == true
	}

	fun closeWelcomeSheet(): Boolean {
		val tag = fragmentTag<WelcomeSheet>()
		val sheet = fragment?.findFragmentByTagRecursive(tag)
			?: activity?.supportFragmentManager?.findFragmentByTag(tag)
			?: return false
		return if (sheet is WelcomeSheet) {
			sheet.dismissAllowingStateLoss()
			true
		} else {
			false
		}
	}

	/** Private utils **/

	private fun startActivity(intent: Intent, options: Bundle? = null) {
		fragment?.also {
			if (it.host != null) {
				it.startActivity(intent, options)
			}
		} ?: activity?.startActivity(intent, options)
	}

	private fun startActivitySafe(intent: Intent): Boolean = try {
		startActivity(intent)
		true
	} catch (_: ActivityNotFoundException) {
		false
	}

	private fun startActivity(activityClass: Class<out Activity>) {
		startActivity(Intent(contextOrNull() ?: return, activityClass))
	}

	private fun getFragmentManager(): FragmentManager? = runCatching {
		fragment?.childFragmentManager ?: activity?.supportFragmentManager
	}.onFailure { exception ->
		exception.printStackTraceDebug()
	}.getOrNull()

	private fun shareLink(link: String, title: String) {
		val context = contextOrNull() ?: return
		ShareCompat.IntentBuilder(context)
			.setText(link)
			.setType(TYPE_TEXT)
			.setChooserTitle(context.getString(R.string.share_s, title.ellipsize(12)))
			.startChooser()
	}

	private fun shareFile(file: File) { // TODO directory sharing support
		val context = contextOrNull() ?: return
		val intentBuilder = ShareCompat.IntentBuilder(context)
			.setType(TYPE_CBZ)
		val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
		intentBuilder.addStream(uri)
		intentBuilder.setChooserTitle(context.getString(R.string.share_s, file.name))
		intentBuilder.startChooser()
	}

	@UiContext
	private fun contextOrNull(): Context? = activity ?: fragment?.context

	private fun getLifecycleOwner(): LifecycleOwner? = activity ?: fragment?.viewLifecycleOwner

	private fun DialogFragment.showDistinct(): Boolean {
		val fm = this@AppRouter.getFragmentManager() ?: return false
		val tag = javaClass.fragmentTag()
		val existing = fm.findFragmentByTag(tag) as? DialogFragment?
		if (existing != null && existing.isVisible && existing.arguments == this.arguments) {
			return false
		}
		show(fm, tag)
		return true
	}

	private fun DialogFragment.show() {
		show(
			this@AppRouter.getFragmentManager() ?: return,
			javaClass.fragmentTag(),
		)
	}

	private fun Fragment.findFragmentByTagRecursive(fragmentTag: String): Fragment? {
		childFragmentManager.findFragmentByTag(fragmentTag)?.let {
			return it
		}
		val parent = parentFragment
		return if (parent != null) {
			parent.findFragmentByTagRecursive(fragmentTag)
		} else {
			parentFragmentManager.findFragmentByTag(fragmentTag)
		}
	}

	companion object {

		fun from(view: View): AppRouter? = runCatching {
			AppRouter(view.findFragment())
		}.getOrElse {
			(view.context.findActivity() as? FragmentActivity)?.let(::AppRouter)
		}

		fun detailsIntent(context: Context, manga: Manga) = Intent(context, DetailsActivity::class.java)
			.putExtra(KEY_MANGA, ParcelableManga(manga))
			.setData(shortMangaUrl(manga.id))

		fun detailsIntent(context: Context, mangaId: Long) = Intent(context, DetailsActivity::class.java)
			.putExtra(KEY_ID, mangaId)
			.setData(shortMangaUrl(mangaId))

		fun listIntent(context: Context, source: MangaSource, filter: MangaListFilter?, sortOrder: SortOrder?): Intent =
			Intent(context, MangaListActivity::class.java)
				.setAction(ACTION_MANGA_EXPLORE)
				.putExtra(KEY_SOURCE, source.name)
				.apply {
					if (!filter.isNullOrEmpty()) {
						putExtra(KEY_FILTER, ParcelableMangaListFilter(filter))
					}
					if (sortOrder != null) {
						putExtra(KEY_SORT_ORDER, sortOrder)
					}
				}

		fun cloudFlareResolveIntent(context: Context, exception: CloudFlareProtectedException): Intent =
			Intent(context, CloudFlareActivity::class.java).apply {
				data = exception.url.toUri()
				putExtra(KEY_SOURCE, exception.source.name)
				exception.headers[CommonHeaders.USER_AGENT]?.let {
					putExtra(KEY_USER_AGENT, it)
				}
			}

		fun browserIntent(
			context: Context,
			url: String,
			source: MangaSource?,
			title: String?
		): Intent = Intent(context, BrowserActivity::class.java)
			.setData(url.toUri())
			.putExtra(KEY_TITLE, title)
			.putExtra(KEY_SOURCE, source?.name)

		fun suggestionsIntent(context: Context) = Intent(context, SuggestionsActivity::class.java)

		fun homeIntent(context: Context) = Intent(context, MainActivity::class.java)

		fun mangaUpdatesIntent(context: Context) = Intent(context, UpdatesActivity::class.java)

		fun readerSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_READER)

		fun suggestionsSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SUGGESTIONS)

		fun trackerSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_TRACKER)

		fun periodicBackupSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_PERIODIC_BACKUP)

		fun discordSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_DISCORD)

		fun proxySettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_PROXY)

		fun historySettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_HISTORY)

		fun sourcesSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCES)

		fun manageSourcesIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_SOURCES)

		fun downloadsSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_DOWNLOADS)

		fun sourceSettingsIntent(context: Context, source: MangaSource): Intent = when (source) {
			is MangaSourceInfo -> sourceSettingsIntent(context, source.mangaSource)
			is ExternalMangaSource -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
				.setData(Uri.fromParts("package", source.packageName, null))

			else -> Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCE)
				.putExtra(KEY_SOURCE, source.name)
		}

		fun sourceAuthIntent(context: Context, source: MangaSource): Intent {
			return Intent(context, SourceAuthActivity::class.java)
				.putExtra(KEY_SOURCE, source.name)
		}

		fun overrideEditIntent(context: Context, manga: Manga): Intent =
			Intent(context, OverrideConfigActivity::class.java)
				.putExtra(KEY_MANGA, ParcelableManga(manga, withDescription = false))

		fun isShareSupported(manga: Manga): Boolean = when {
			manga.isBroken -> false
			manga.isLocal -> manga.url.toUri().toFileOrNull() != null
			else -> true
		}

		fun shortMangaUrl(mangaId: Long): Uri = Uri.Builder()
			.scheme("kotatsu")
			.path("manga")
			.appendQueryParameter("id", mangaId.toString())
			.build()

		const val KEY_DATA = "data"
		const val KEY_ENTRIES = "entries"
		const val KEY_ERROR = "error"
		const val KEY_EXCLUDE = "exclude"
		const val KEY_FILE = "file"
		const val KEY_FILTER = "filter"
		const val KEY_ID = "id"
		const val KEY_INDEX = "index"
		const val KEY_IS_BOTTOMTAB = "is_btab"
		const val KEY_KIND = "kind"
		const val KEY_LIST_SECTION = "list_section"
		const val KEY_MANGA = "manga"
		const val KEY_MANGA_LIST = "manga_list"
		const val KEY_PAGES = "pages"
		const val KEY_PREVIEW = "preview"
		const val KEY_QUERY = "query"
		const val KEY_READER_MODE = "reader_mode"
		const val KEY_SORT_ORDER = "sort_order"
		const val KEY_SOURCE = "source"
		const val KEY_TAB = "tab"
		const val KEY_TITLE = "title"
		const val KEY_URL = "url"
		const val KEY_USER_AGENT = "user_agent"

		const val ACTION_HISTORY = "${BuildConfig.APPLICATION_ID}.action.MANAGE_HISTORY"
		const val ACTION_MANAGE_DOWNLOADS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_DOWNLOADS"
		const val ACTION_MANAGE_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES_LIST"
		const val ACTION_MANGA_EXPLORE = "${BuildConfig.APPLICATION_ID}.action.EXPLORE_MANGA"
		const val ACTION_PROXY = "${BuildConfig.APPLICATION_ID}.action.MANAGE_PROXY"
		const val ACTION_READER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"
		const val ACTION_SOURCE = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCE_SETTINGS"
		const val ACTION_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES"
		const val ACTION_MANAGE_DISCORD = "${BuildConfig.APPLICATION_ID}.action.MANAGE_DISCORD"
		const val ACTION_SUGGESTIONS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SUGGESTIONS"
		const val ACTION_TRACKER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_TRACKER"
		const val ACTION_PERIODIC_BACKUP = "${BuildConfig.APPLICATION_ID}.action.MANAGE_PERIODIC_BACKUP"

		private const val ACCOUNT_KEY = "account"
		private const val ACTION_ACCOUNT_SYNC_SETTINGS = "android.settings.ACCOUNT_SYNC_SETTINGS"
		private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

		private const val TYPE_TEXT = "text/plain"
		private const val TYPE_IMAGE = "image/*"
		private const val TYPE_CBZ = "application/x-cbz"

		private fun Class<out Fragment>.fragmentTag() = name // TODO

		private inline fun <reified F : Fragment> fragmentTag() = F::class.java.fragmentTag()
	}
}
