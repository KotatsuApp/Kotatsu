package org.koitharu.kotatsu.shikimori.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import coil.transform.CircleCropTransformation
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.databinding.SheetShikiSelectorBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.shikimori.data.model.ShikimoriManga
import org.koitharu.kotatsu.shikimori.ui.selector.adapter.ShikiMangaSelectionDecoration
import org.koitharu.kotatsu.shikimori.ui.selector.adapter.ShikimoriSelectorAdapter
import org.koitharu.kotatsu.utils.BottomSheetToolbarController
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.withArgs

class ShikimoriSelectorBottomSheet :
	BaseBottomSheet<SheetShikiSelectorBinding>(),
	OnListItemClickListener<ShikimoriManga>,
	PaginationScrollListener.Callback,
	View.OnClickListener {

	private val viewModel by viewModel<ShikimoriSelectorViewModel> {
		parametersOf(requireNotNull(requireArguments().getParcelable<ParcelableManga>(MangaIntent.KEY_MANGA)).manga)
	}

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): SheetShikiSelectorBinding {
		return SheetShikiSelectorBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.subtitle = viewModel.manga.title
		binding.toolbar.setNavigationOnClickListener { dismiss() }
		addBottomSheetCallback(BottomSheetToolbarController(binding.toolbar))
		val listAdapter = ShikimoriSelectorAdapter(viewLifecycleOwner, get(), this)
		val decoration = ShikiMangaSelectionDecoration(view.context)
		with(binding.recyclerView) {
			adapter = listAdapter
			addItemDecoration(decoration)
			addOnScrollListener(PaginationScrollListener(4, this@ShikimoriSelectorBottomSheet))
		}
		binding.imageViewUser.setOnClickListener(this)

		viewModel.content.observe(viewLifecycleOwner) { listAdapter.items = it }
		viewModel.selectedItemId.observe(viewLifecycleOwner) {
			decoration.checkedItemId = it
			binding.recyclerView.invalidateItemDecorations()
		}
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.avatar.observe(viewLifecycleOwner, ::setUserAvatar)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.imageView_user -> startActivity(SettingsActivity.newShikimoriSettingsIntent(v.context))
		}
	}

	override fun onItemClick(item: ShikimoriManga, view: View) {
		viewModel.selectedItemId.value = item.id
	}

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	private fun onError(e: Throwable) {
		Toast.makeText(requireContext(), e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
		if (viewModel.isEmpty) {
			dismissAllowingStateLoss()
		}
	}

	private fun setUserAvatar(url: String?) {
		val iconSize = resources.getDimensionPixelSize(R.dimen.action_bar_item_size)
		binding.imageViewUser.newImageRequest(url)
			.transformations(CircleCropTransformation())
			.size(iconSize, iconSize)
			.enqueueWith(get())
	}

	companion object {

		private const val TAG = "ShikimoriSelectorBottomSheet"

		fun show(fm: FragmentManager, manga: Manga) =
			ShikimoriSelectorBottomSheet().withArgs(1) {
				putParcelable(MangaIntent.KEY_MANGA, ParcelableManga(manga, withChapters = false))
			}.show(fm, TAG)
	}
}