package org.koitharu.kotatsu.search.ui.multi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.core.util.ext.invalidateNestedItemDecorations
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.databinding.ActivitySearchMultiBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.worker.DownloadStartedObserver
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.search.ui.multi.adapter.MultiSearchAdapter
import javax.inject.Inject

@AndroidEntryPoint
class MultiSearchActivity :
	BaseActivity<ActivitySearchMultiBinding>(),
	MangaListListener,
	ListSelectionController.Callback2 {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<MultiSearchViewModel>()
	private lateinit var adapter: MultiSearchAdapter
	private lateinit var selectionController: ListSelectionController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchMultiBinding.inflate(layoutInflater))
		window.statusBarColor = ContextCompat.getColor(this, R.color.dim_statusbar)

		val itemCLickListener = object : OnListItemClickListener<MultiSearchListModel> {
			override fun onItemClick(item: MultiSearchListModel, view: View) {
				startActivity(SearchActivity.newIntent(view.context, item.source, viewModel.query.value))
			}
		}
		val sizeResolver = ItemSizeResolver(resources, settings)
		val selectionDecoration = MangaSelectionDecoration(this)
		selectionController = ListSelectionController(
			activity = this,
			decoration = selectionDecoration,
			registryOwner = this,
			callback = this,
		)
		adapter = MultiSearchAdapter(
			lifecycleOwner = this,
			coil = coil,
			listener = this,
			itemClickListener = itemCLickListener,
			sizeResolver = sizeResolver,
			selectionDecoration = selectionDecoration,
		)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.recyclerView.setHasFixedSize(true)

		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setSubtitle(R.string.search_results)
		}

		viewModel.query.observe(this) { title = it }
		viewModel.list.observe(this) { adapter.items = it }
		viewModel.onError.observe(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
		viewModel.onDownloadStarted.observe(this, DownloadStartedObserver(viewBinding.recyclerView))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	override fun onItemClick(item: Manga, view: View) {
		if (!selectionController.onItemClick(item.id)) {
			val intent = DetailsActivity.newIntent(this, item)
			startActivity(intent)
		}
	}

	override fun onItemLongClick(item: Manga, view: View): Boolean {
		return selectionController.onItemLongClick(item.id)
	}

	override fun onReadClick(manga: Manga, view: View) {
		if (!selectionController.onItemClick(manga.id)) {
			val intent = ReaderActivity.newIntent(this, manga)
			startActivity(intent, scaleUpActivityOptionsOf(view).toBundle())
		}
	}

	override fun onTagClick(manga: Manga, tag: MangaTag, view: View) {
		if (!selectionController.onItemClick(manga.id)) {
			val intent = MangaListActivity.newIntent(this, setOf(tag))
			startActivity(intent)
		}
	}

	override fun onRetryClick(error: Throwable) {
		viewModel.doSearch(viewModel.query.value.orEmpty())
	}

	override fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	override fun onFilterClick(view: View?) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onListHeaderClick(item: ListHeader, view: View) = Unit

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		viewBinding.recyclerView.invalidateNestedItemDecorations()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_share -> {
				ShareHelper(this).shareMangaLinks(collectSelectedItems())
				mode.finish()
				true
			}

			R.id.action_favourite -> {
				FavouriteCategoriesBottomSheet.show(supportFragmentManager, collectSelectedItems())
				mode.finish()
				true
			}

			R.id.action_save -> {
				viewModel.download(collectSelectedItems())
				mode.finish()
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
			Intent(context, MultiSearchActivity::class.java)
				.putExtra(EXTRA_QUERY, query)
	}
}
