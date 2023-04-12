package org.koitharu.kotatsu.shelf.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.base.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.FragmentShelfBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.favourites.ui.FavouritesActivity
import org.koitharu.kotatsu.history.ui.HistoryActivity
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.shelf.ui.adapter.ShelfAdapter
import org.koitharu.kotatsu.shelf.ui.adapter.ShelfListEventListener
import org.koitharu.kotatsu.shelf.ui.model.ShelfSectionModel
import org.koitharu.kotatsu.tracker.ui.updates.UpdatesActivity
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import javax.inject.Inject

@AndroidEntryPoint
class ShelfFragment :
	BaseFragment<FragmentShelfBinding>(),
	RecyclerViewOwner,
	ShelfListEventListener {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<ShelfViewModel>()
	private var adapter: ShelfAdapter? = null
	private var selectionController: SectionedSelectionController<ShelfSectionModel>? = null

	override val recyclerView: RecyclerView
		get() = binding.recyclerView

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentShelfBinding {
		return FragmentShelfBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val sizeResolver = ItemSizeResolver(resources, settings)
		selectionController = SectionedSelectionController(
			activity = requireActivity(),
			owner = this,
			callback = ShelfSelectionCallback(binding.recyclerView, childFragmentManager, viewModel),
		)
		adapter = ShelfAdapter(
			lifecycleOwner = viewLifecycleOwner,
			coil = coil,
			listener = this,
			sizeResolver = sizeResolver,
			selectionController = checkNotNull(selectionController),
		)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.setHasFixedSize(true)
		addMenuProvider(ShelfMenuProvider(view.context, childFragmentManager, viewModel))

		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onError.observe(viewLifecycleOwner, SnackbarErrorObserver(binding.recyclerView, this))
		viewModel.onActionDone.observe(viewLifecycleOwner, ReversibleActionObserver(binding.recyclerView))
	}

	override fun onDestroyView() {
		super.onDestroyView()
		adapter = null
		selectionController = null
	}

	override fun onItemClick(item: Manga, section: ShelfSectionModel, view: View) {
		if (selectionController?.onItemClick(section, item.id) != true) {
			val intent = DetailsActivity.newIntent(view.context, item)
			startActivity(intent)
		}
	}

	override fun onItemLongClick(item: Manga, section: ShelfSectionModel, view: View): Boolean {
		return selectionController?.onItemLongClick(section, item.id) ?: false
	}

	override fun onSectionClick(section: ShelfSectionModel, view: View) {
		selectionController?.clear()
		val intent = when (section) {
			is ShelfSectionModel.History -> HistoryActivity.newIntent(view.context)
			is ShelfSectionModel.Favourites -> FavouritesActivity.newIntent(view.context, section.category)
			is ShelfSectionModel.Updated -> UpdatesActivity.newIntent(view.context)
			is ShelfSectionModel.Local -> MangaListActivity.newIntent(view.context, MangaSource.LOCAL)
		}
		startActivity(intent)
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() {
		val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			Settings.Panel.ACTION_INTERNET_CONNECTIVITY
		} else {
			Settings.ACTION_WIRELESS_SETTINGS
		}
		startActivity(Intent(action))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	private fun onListChanged(list: List<ListModel>) {
		adapter?.items = list
	}

	companion object {

		fun newInstance() = ShelfFragment()
	}
}
