package org.koitharu.kotatsu.favourites.ui.categories.select

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import coil3.ImageLoader
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.disposeImageRequest
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.joinToStringWithLimit
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetFavoriteCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.select.adapter.MangaCategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@AndroidEntryPoint
class FavoriteDialog : AlertDialogFragment<SheetFavoriteCategoriesBinding>(),
	OnListItemClickListener<MangaCategoryItem>, DialogInterface.OnClickListener {

	private val viewModel by viewModels<FavoriteSheetViewModel>()

	@Inject
	lateinit var coil: ImageLoader

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = SheetFavoriteCategoriesBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setPositiveButton(R.string.done, null)
			.setNeutralButton(R.string.manage, this)
	}

	override fun onViewBindingCreated(
		binding: SheetFavoriteCategoriesBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = MangaCategoriesAdapter(coil, viewLifecycleOwner, this)
		binding.recyclerViewCategories.adapter = adapter
		viewModel.content.observe(viewLifecycleOwner, adapter)
		viewModel.onError.observeEvent(viewLifecycleOwner, ::onError)
		bindHeader()
	}

	override fun onItemClick(item: MangaCategoryItem, view: View) {
		viewModel.setChecked(item.category.id, item.checkedState != MaterialCheckBox.STATE_CHECKED)
	}

	override fun onClick(dialog: DialogInterface?, which: Int) {
		startActivity(Intent(context ?: return, FavouriteCategoriesActivity::class.java))
	}

	private fun onError(e: Throwable) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	private fun bindHeader() {
		val manga = viewModel.manga
		val binding = viewBinding ?: return
		val backgroundColor = binding.root.context.getThemeColor(android.R.attr.colorBackground)
		ImageViewCompat.setImageTintList(
			binding.imageViewCover3,
			ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153)),
		)
		ImageViewCompat.setImageTintList(
			binding.imageViewCover2,
			ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76)),
		)
		binding.imageViewCover2.backgroundTintList =
			ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76))
		binding.imageViewCover3.backgroundTintList =
			ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153))
		val fallback = ColorDrawable(Color.TRANSPARENT)
		val coverViews = arrayOf(binding.imageViewCover1, binding.imageViewCover2, binding.imageViewCover3)
		val crossFadeDuration = binding.root.context.getAnimationDuration(R.integer.config_defaultAnimTime).toInt()

		binding.textViewTitle.text = manga.joinToStringWithLimit(binding.root.context, 92) { it.title }

		repeat(coverViews.size) { i ->
			val m = manga.getOrNull(i)
			val view = coverViews[i]
			view.isVisible = m != null
			if (m == null) {
				view.disposeImageRequest()
			} else {
				view.newImageRequest(viewLifecycleOwner, m.coverUrl)?.run {
					placeholder(R.drawable.ic_placeholder)
					fallback(fallback)
					mangaSourceExtra(m.source)
					crossfade(crossFadeDuration * (i + 1))
					error(R.drawable.ic_error_placeholder)
					allowRgb565(true)
					enqueueWith(coil)
				}
			}
		}
	}

	companion object {

		private const val TAG = "FavoriteSheet"
		const val KEY_MANGA_LIST = "manga_list"

		fun show(fm: FragmentManager, manga: Manga) = show(fm, setOf(manga))

		fun show(fm: FragmentManager, manga: Collection<Manga>) = FavoriteDialog().withArgs(1) {
			putParcelableArrayList(
				KEY_MANGA_LIST,
				manga.mapTo(ArrayList(manga.size), ::ParcelableManga),
			)
		}.showDistinct(fm, TAG)
	}
}
