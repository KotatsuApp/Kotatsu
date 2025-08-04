package org.koitharu.kotatsu.list.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.FragmentPreviewBinding
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import org.koitharu.kotatsu.search.ui.MangaListActivity

@AndroidEntryPoint
class PreviewFragment : BaseFragment<FragmentPreviewBinding>(), View.OnClickListener, ChipsView.OnChipClickListener {

	private val viewModel: PreviewViewModel by viewModels()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPreviewBinding {
		return FragmentPreviewBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: FragmentPreviewBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.buttonClose.isVisible = activity is MangaListActivity
		binding.buttonClose.setOnClickListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethodCompat.getInstance()
		binding.chipsTags.onChipClickListener = this
		binding.textViewAuthor.setOnClickListener(this)
		binding.imageViewCover.setOnClickListener(this)
		binding.buttonOpen.setOnClickListener(this)
		binding.buttonRead.setOnClickListener(this)

		viewModel.manga.observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.footer.observe(viewLifecycleOwner, ::onFooterUpdated)
		viewModel.tagsChips.observe(viewLifecycleOwner, ::onTagsChipsChanged)
		viewModel.description.observe(viewLifecycleOwner, ::onDescriptionChanged)
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat = insets

	override fun onClick(v: View) {
		val manga = viewModel.manga.value
		when (v.id) {
			R.id.button_close -> closeSelf()
			R.id.button_open -> router.openDetails(manga)
			R.id.button_read -> router.openReader(manga)

			R.id.textView_author -> router.showAuthorDialog(
				author = manga.authors.firstOrNull() ?: return,
				source = manga.source,
			)

			R.id.imageView_cover -> router.openImage(
				url = manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl } ?: return,
				source = manga.source,
				anchor = v,
			)
		}
	}

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag ?: return
		val filter = FilterCoordinator.find(this)
		if (filter == null) {
			router.openList(tag)
		} else {
			filter.toggleTag(tag, true)
			closeSelf()
		}
	}

	private fun onMangaUpdated(manga: Manga) {
		with(requireViewBinding()) {
			// Main
			loadCover(manga)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitles.firstOrNull()
			textViewAuthor.textAndVisible = manga.authors.firstOrNull()
			if (manga.hasRating) {
				ratingBar.rating = manga.rating * ratingBar.numStars
				ratingBar.isVisible = true
			} else {
				ratingBar.isVisible = false
			}
		}
	}

	private fun onFooterUpdated(footer: PreviewViewModel.FooterInfo?) {
		with(requireViewBinding()) {
			buttonRead.isEnabled = footer != null
			buttonRead.setText(
				when {
					footer == null -> R.string.loading_
					footer.isIncognito -> R.string.incognito
					footer.isInProgress() -> R.string._continue
					else -> R.string.read
				},
			)
		}
	}

	private fun onDescriptionChanged(description: CharSequence?) {
		val tv = viewBinding?.textViewDescription ?: return
		when {
			description == null -> tv.setText(R.string.loading_)
			description.isBlank() -> tv.setText(R.string.no_description)
			else -> tv.setText(description, TextView.BufferType.NORMAL)
		}
	}

	private fun loadCover(manga: Manga) {
		val imageUrl = manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl }
		requireViewBinding().imageViewCover.setImageAsync(imageUrl, manga)
	}

	private fun onTagsChipsChanged(chips: List<ChipsView.ChipModel>) {
		requireViewBinding().chipsTags.setChips(chips)
	}

	private fun closeSelf() {
		((activity as? MangaListActivity)?.hidePreview())
	}
}
