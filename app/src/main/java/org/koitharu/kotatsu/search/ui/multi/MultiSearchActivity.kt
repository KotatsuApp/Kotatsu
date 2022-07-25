package org.koitharu.kotatsu.search.ui.multi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ActivitySearchMultiBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.search.ui.multi.adapter.MultiSearchAdapter
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.assistedViewModels
import org.koitharu.kotatsu.utils.ext.invalidateNestedItemDecorations

@AndroidEntryPoint
class MultiSearchActivity :
	BaseActivity<ActivitySearchMultiBinding>(),
	MangaListListener,
	ListSelectionController.Callback {

	@Inject
	lateinit var viewModelFactory: MultiSearchViewModel.Factory

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by assistedViewModels<MultiSearchViewModel> {
		viewModelFactory.create(intent.getStringExtra(EXTRA_QUERY).orEmpty())
	}
	private lateinit var adapter: MultiSearchAdapter
	private lateinit var selectionController: ListSelectionController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchMultiBinding.inflate(layoutInflater))

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
		binding.recyclerView.adapter = adapter
		binding.recyclerView.setHasFixedSize(true)

		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setSubtitle(R.string.search_results)
		}

		viewModel.query.observe(this) { title = it }
		viewModel.list.observe(this) { adapter.items = it }
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
			left = insets.left,
			right = insets.right,
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

	override fun onRetryClick(error: Throwable) {
		viewModel.doSearch(viewModel.query.value.orEmpty())
	}

	override fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	override fun onFilterClick(view: View?) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onListHeaderClick(item: ListHeader, view: View) = Unit

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.title = selectionController.count.toString()
		return true
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
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
				DownloadService.confirmAndStart(this, collectSelectedItems())
				mode.finish()
				true
			}
			else -> false
		}
	}

	override fun onSelectionChanged(count: Int) {
		binding.recyclerView.invalidateNestedItemDecorations()
	}

	private fun collectSelectedItems(): Set<Manga> {
		return viewModel.getItems(selectionController.peekCheckedIds())
	}

	companion object {

		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, query: String) =
			Intent(context, MultiSearchActivity::class.java)
				.putExtra(EXTRA_QUERY, query)
	}
}
