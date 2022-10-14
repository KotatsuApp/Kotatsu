package org.koitharu.kotatsu.shelf.ui

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.flattenTo
import org.koitharu.kotatsu.shelf.ui.model.ShelfSectionModel
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.invalidateNestedItemDecorations

class ShelfSelectionCallback(
	private val recyclerView: RecyclerView,
	private val fragmentManager: FragmentManager,
	private val viewModel: ShelfViewModel,
) : SectionedSelectionController.Callback<ShelfSectionModel> {

	private val context: Context
		get() = recyclerView.context

	override fun onCreateActionMode(
		controller: SectionedSelectionController<ShelfSectionModel>,
		mode: ActionMode,
		menu: Menu,
	): Boolean {
		mode.menuInflater.inflate(R.menu.mode_shelf, menu)
		return true
	}

	override fun onPrepareActionMode(
		controller: SectionedSelectionController<ShelfSectionModel>,
		mode: ActionMode,
		menu: Menu,
	): Boolean {
		val checkedIds = controller.peekCheckedIds()
		menu.findItem(R.id.action_remove).isVisible = checkedIds.none { (key, _) -> key is ShelfSectionModel.Updated }
			&& checkedIds.count { (_, v) -> v.isNotEmpty() } == 1
		return super.onPrepareActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(
		controller: SectionedSelectionController<ShelfSectionModel>,
		mode: ActionMode,
		item: MenuItem,
	): Boolean {
		return when (item.itemId) {
			R.id.action_share -> {
				ShareHelper(context).shareMangaLinks(collectSelectedItems(controller))
				mode.finish()
				true
			}

			R.id.action_favourite -> {
				FavouriteCategoriesBottomSheet.show(fragmentManager, collectSelectedItems(controller))
				mode.finish()
				true
			}

			R.id.action_save -> {
				DownloadService.confirmAndStart(context, collectSelectedItems(controller))
				mode.finish()
				true
			}

			R.id.action_remove -> {
				val (group, ids) = controller.snapshot().entries.singleOrNull { it.value.isNotEmpty() } ?: return false
				when (group) {
					is ShelfSectionModel.Favourites -> viewModel.removeFromFavourites(group.category, ids)
					is ShelfSectionModel.History -> viewModel.removeFromHistory(ids)
					is ShelfSectionModel.Updated -> return false
				}
				mode.finish()
				true
			}

			else -> false
		}
	}

	override fun onSelectionChanged(controller: SectionedSelectionController<ShelfSectionModel>, count: Int) {
		recyclerView.invalidateNestedItemDecorations()
	}

	override fun onCreateItemDecoration(
		controller: SectionedSelectionController<ShelfSectionModel>,
		section: ShelfSectionModel,
	): AbstractSelectionItemDecoration = MangaSelectionDecoration(context)

	private fun collectSelectedItemsMap(
		controller: SectionedSelectionController<ShelfSectionModel>,
	): Map<ShelfSectionModel, Set<Manga>> {
		val snapshot = controller.peekCheckedIds()
		if (snapshot.isEmpty()) {
			return emptyMap()
		}
		return snapshot.mapValues { (_, ids) -> viewModel.getManga(ids) }
	}

	private fun collectSelectedItems(
		controller: SectionedSelectionController<ShelfSectionModel>,
	): Set<Manga> {
		val snapshot = controller.peekCheckedIds()
		if (snapshot.isEmpty()) {
			return emptySet()
		}
		return viewModel.getManga(snapshot.values.flattenTo(HashSet()))
	}
}
