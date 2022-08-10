package org.koitharu.kotatsu.library.ui

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
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.flattenTo
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.invalidateNestedItemDecorations

class LibrarySelectionCallback(
	private val recyclerView: RecyclerView,
	private val fragmentManager: FragmentManager,
	private val viewModel: LibraryViewModel,
) : SectionedSelectionController.Callback<LibrarySectionModel> {

	private val context: Context
		get() = recyclerView.context

	override fun onCreateActionMode(
		controller: SectionedSelectionController<LibrarySectionModel>,
		mode: ActionMode,
		menu: Menu,
	): Boolean {
		mode.menuInflater.inflate(R.menu.mode_library, menu)
		return true
	}

	override fun onPrepareActionMode(
		controller: SectionedSelectionController<LibrarySectionModel>,
		mode: ActionMode,
		menu: Menu,
	): Boolean {
		menu.findItem(R.id.action_remove).isVisible =
			controller.peekCheckedIds().count { (_, v) -> v.isNotEmpty() } == 1
		return super.onPrepareActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(
		controller: SectionedSelectionController<LibrarySectionModel>,
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
					is LibrarySectionModel.Favourites -> viewModel.removeFromFavourites(group.category, ids)
					is LibrarySectionModel.History -> viewModel.removeFromHistory(ids)
				}
				mode.finish()
				true
			}
			else -> false
		}
	}

	override fun onSelectionChanged(controller: SectionedSelectionController<LibrarySectionModel>, count: Int) {
		recyclerView.invalidateNestedItemDecorations()
	}

	override fun onCreateItemDecoration(
		controller: SectionedSelectionController<LibrarySectionModel>,
		section: LibrarySectionModel,
	): AbstractSelectionItemDecoration = MangaSelectionDecoration(context)

	private fun collectSelectedItemsMap(
		controller: SectionedSelectionController<LibrarySectionModel>,
	): Map<LibrarySectionModel, Set<Manga>> {
		val snapshot = controller.peekCheckedIds()
		if (snapshot.isEmpty()) {
			return emptyMap()
		}
		return snapshot.mapValues { (_, ids) -> viewModel.getManga(ids) }
	}

	private fun collectSelectedItems(
		controller: SectionedSelectionController<LibrarySectionModel>,
	): Set<Manga> {
		val snapshot = controller.peekCheckedIds()
		if (snapshot.isEmpty()) {
			return emptySet()
		}
		return viewModel.getManga(snapshot.values.flattenTo(HashSet()))
	}
}
