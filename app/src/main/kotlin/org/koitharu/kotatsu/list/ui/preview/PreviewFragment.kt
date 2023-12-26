package org.koitharu.kotatsu.list.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.crossfade
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.FragmentPreviewBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.filter.ui.FilterOwner
import org.koitharu.kotatsu.image.ui.ImageActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.search.ui.SearchActivity
import javax.inject.Inject

@AndroidEntryPoint
class PreviewFragment : BaseFragment<FragmentPreviewBinding>(), View.OnClickListener, ChipsView.OnChipClickListener {

	@Inject
	lateinit var coil: ImageLoader

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

	override fun onClick(v: View) {
		val manga = viewModel.manga.value
		when (v.id) {
			R.id.button_close -> closeSelf()
			R.id.button_open -> startActivity(
				DetailsActivity.newIntent(v.context, manga),
			)

			R.id.button_read -> {
				startActivity(
					ReaderActivity.IntentBuilder(v.context)
						.manga(manga)
						.build(),
				)
			}

			R.id.textView_author -> startActivity(
				SearchActivity.newIntent(
					context = v.context,
					source = manga.source,
					query = manga.author ?: return,
				),
			)

			R.id.imageView_cover -> startActivity(
				ImageActivity.newIntent(
					v.context,
					manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl },
					manga.source,
				),
				scaleUpActivityOptionsOf(v),
			)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag ?: return
		val filter = (activity as? FilterOwner)?.filter
		if (filter == null) {
			startActivity(MangaListActivity.newIntent(requireContext(), setOf(tag)))
		} else {
			filter.setTag(tag, true)
			closeSelf()
		}
	}

	private fun onMangaUpdated(manga: Manga) {
		with(requireViewBinding()) {
			// Main
			loadCover(manga)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitle
			textViewAuthor.textAndVisible = manga.author
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
			toolbarBottom.isVisible = footer != null
			if (footer == null) {
				return
			}
			toolbarBottom.title = when {
				footer.isInProgress() -> {
					getString(R.string.chapter_d_of_d, footer.currentChapter, footer.totalChapters)
				}

				footer.totalChapters > 0 -> {
					resources.getQuantityString(R.plurals.chapters, footer.totalChapters, footer.totalChapters)
				}

				else -> {
					getString(R.string.no_chapters)
				}
			}
			buttonRead.isEnabled = footer.totalChapters > 0
			buttonRead.setIconResource(
				when {
					footer.isIncognito -> R.drawable.ic_incognito
					footer.isInProgress() -> R.drawable.ic_play
					else -> R.drawable.ic_read
				},
			)
			buttonRead.setText(
				if (footer.isInProgress()) {
					R.string._continue
				} else {
					R.string.read
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
		val lastResult = CoilUtils.result(requireViewBinding().imageViewCover)
		if (lastResult is SuccessResult && lastResult.request.data == imageUrl) {
			return
		}
		val request = ImageRequest.Builder(context ?: return)
			.target(requireViewBinding().imageViewCover)
			.size(CoverSizeResolver(requireViewBinding().imageViewCover))
			.data(imageUrl)
			.tag(manga.source)
			.crossfade(requireContext())
			.lifecycle(viewLifecycleOwner)
			.placeholderMemoryCacheKey(manga.coverUrl)
		val previousDrawable = lastResult?.drawable
		if (previousDrawable != null) {
			request.fallback(previousDrawable)
				.placeholder(previousDrawable)
				.error(previousDrawable)
		} else {
			request.fallback(R.drawable.ic_placeholder)
				.placeholder(R.drawable.ic_placeholder)
				.error(R.drawable.ic_error_placeholder)
		}
		request.enqueueWith(coil)
	}

	private fun onTagsChipsChanged(chips: List<ChipsView.ChipModel>) {
		requireViewBinding().chipsTags.setChips(chips)
	}

	private fun closeSelf() {
		((activity as? MangaListActivity)?.hidePreview())
	}
}
