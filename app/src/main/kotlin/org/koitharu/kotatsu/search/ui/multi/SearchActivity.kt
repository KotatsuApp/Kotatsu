package org.koitharu.kotatsu.search.ui.multi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.widgets.TipView
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.core.util.ext.invalidateNestedItemDecorations
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivitySearchBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.dialog.DownloadDialogFragment
import org.koitharu.kotatsu.favourites.ui.categories.select.FavoriteSheet
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.size.DynamicItemSizeResolver
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.multi.adapter.SearchAdapter
import javax.inject.Inject

@AndroidEntryPoint
class SearchActivity :
	BaseActivity<ActivitySearchBinding>(),
	MangaListListener,
	ListSelectionController.Callback {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<SearchViewModel>()
	private lateinit var selectionController: ListSelectionController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchBinding.inflate(layoutInflater))
		title = viewModel.query

		val itemCLickListener = OnListItemClickListener<SearchResultsListModel> { item, view ->
			startActivity(
				MangaListActivity.newIntent(
					view.context,
					item.source,
					MangaListFilter(query = viewModel.query),
				),
			)
		}
		val sizeResolver = DynamicItemSizeResolver(resources, settings, adjustWidth = true)
		val selectionDecoration = MangaSelectionDecoration(this)
		selectionController = ListSelectionController(
			appCompatDelegate = delegate,
			decoration = selectionDecoration,
			registryOwner = this,
			callback = this,
		)
		val adapter = SearchAdapter(
			lifecycleOwner = this,
			coil = coil,
			listener = this,
			itemClickListener = itemCLickListener,
			sizeResolver = sizeResolver,
			selectionDecoration = selectionDecoration,
		)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.recyclerView.setHasFixedSize(true)
		viewBinding.recyclerView.addItemDecoration(TypedListSpacingDecoration(this, true))

		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setSubtitle(R.string.search_results)
		}

		viewModel.list.observe(this, adapter)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))

		DownloadDialogFragment.registerCallback(this, viewBinding.recyclerView)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.recyclerView.updatePadding(
			bottom = insets.bottom + viewBinding.recyclerView.paddingTop,
		)
	}

	override fun onItemClick(item: Manga, view: View) {
		if (!selectionController.onItemClick(item.id)) {
			val intent = DetailsActivity.newIntent(this, item)
			startActivity(intent)
		}
	}

	override fun onItemLongClick(item: Manga, view: View): Boolean {
		return selectionController.onItemLongClick(view, item.id)
	}

	override fun onItemContextClick(item: Manga, view: View): Boolean {
		return selectionController.onItemContextClick(view, item.id)
	}

	override fun onReadClick(manga: Manga, view: View) {
		if (!selectionController.onItemClick(manga.id)) {
			val intent = IntentBuilder(this).manga(manga).build()
			startActivity(intent)
		}
	}

	override fun onTagClick(manga: Manga, tag: MangaTag, view: View) {
		if (!selectionController.onItemClick(manga.id)) {
			val intent = MangaListActivity.newIntent(this, manga.source, MangaListFilter(tags = setOf(tag)))
			startActivity(intent)
		}
	}

	override fun onRetryClick(error: Throwable) {
		viewModel.retry()
	}

	override fun onFilterOptionClick(option: ListFilterOption) = Unit

	override fun onFilterClick(view: View?) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onListHeaderClick(item: ListHeader, view: View) = Unit

	override fun onPrimaryButtonClick(tipView: TipView) = Unit

	override fun onSecondaryButtonClick(tipView: TipView) = Unit

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		viewBinding.recyclerView.invalidateNestedItemDecorations()
	}

	override fun onCreateActionMode(
		controller: ListSelectionController,
		menuInflater: MenuInflater,
		menu: Menu
	): Boolean {
		menuInflater.inflate(R.menu.mode_remote, menu)
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode?, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_share -> {
				ShareHelper(this).shareMangaLinks(collectSelectedItems())
				mode?.finish()
				true
			}

			R.id.action_favourite -> {
				FavoriteSheet.show(supportFragmentManager, collectSelectedItems())
				mode?.finish()
				true
			}

			R.id.action_save -> {
				DownloadDialogFragment.show(supportFragmentManager, collectSelectedItems())
				mode?.finish()
				true
			}

			else -> false
		}
	}

	private fun collectSelectedItems(): Set<Manga> {
		return viewModel.getItems(selectionController.peekCheckedIds())
	}

	companion object {

		const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, query: String) =
			Intent(context, SearchActivity::class.java)
				.putExtra(EXTRA_QUERY, query)
	}
}
