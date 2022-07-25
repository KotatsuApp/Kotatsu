package org.koitharu.kotatsu.explore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.bookmarks.ui.BookmarksActivity
import org.koitharu.kotatsu.databinding.FragmentExploreBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.explore.ui.adapter.ExploreAdapter
import org.koitharu.kotatsu.explore.ui.adapter.ExploreListEventListener
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.history.ui.HistoryActivity
import org.koitharu.kotatsu.main.ui.BottomNavOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.suggestions.ui.SuggestionsActivity
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

@AndroidEntryPoint
class ExploreFragment :
	BaseFragment<FragmentExploreBinding>(),
	RecyclerViewOwner,
	ExploreListEventListener,
	OnListItemClickListener<ExploreItem.Source> {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<ExploreViewModel>()
	private var exploreAdapter: ExploreAdapter? = null
	private var paddingHorizontal = 0

	override val recyclerView: RecyclerView
		get() = binding.recyclerView

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentExploreBinding {
		return FragmentExploreBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		exploreAdapter = ExploreAdapter(coil, viewLifecycleOwner, this, this)
		with(binding.recyclerView) {
			adapter = exploreAdapter
			setHasFixedSize(true)
			val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
			paddingHorizontal = spacing
		}
		viewModel.content.observe(viewLifecycleOwner) {
			exploreAdapter?.items = it
		}
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.onOpenManga.observe(viewLifecycleOwner, ::onOpenManga)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		exploreAdapter = null
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.recyclerView.updatePadding(
			left = insets.left + paddingHorizontal,
			right = insets.right + paddingHorizontal,
			bottom = insets.bottom,
		)
	}

	override fun onManageClick(view: View) {
		startActivity(SettingsActivity.newManageSourcesIntent(view.context))
	}

	override fun onClick(v: View) {
		val intent = when (v.id) {
			R.id.button_history -> HistoryActivity.newIntent(v.context)
			R.id.button_local -> MangaListActivity.newIntent(v.context, MangaSource.LOCAL)
			R.id.button_bookmarks -> BookmarksActivity.newIntent(v.context)
			R.id.button_suggestions -> SuggestionsActivity.newIntent(v.context)
			R.id.button_favourites -> FavouriteCategoriesActivity.newIntent(v.context)
			R.id.button_random -> {
				viewModel.openRandom()
				return
			}
			else -> return
		}
		startActivity(intent)
	}

	override fun onItemClick(item: ExploreItem.Source, view: View) {
		val intent = MangaListActivity.newIntent(view.context, item.source)
		startActivity(intent)
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = onManageClick(requireView())

	private fun onError(e: Throwable) {
		val snackbar = Snackbar.make(
			binding.recyclerView,
			e.getDisplayMessage(resources),
			Snackbar.LENGTH_SHORT,
		)
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		snackbar.show()
	}

	private fun onOpenManga(manga: Manga) {
		val intent = DetailsActivity.newIntent(context ?: return, manga)
		startActivity(intent)
	}

	companion object {

		fun newInstance() = ExploreFragment()
	}
}
