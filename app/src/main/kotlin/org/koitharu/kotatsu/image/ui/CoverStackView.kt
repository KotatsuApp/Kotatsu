package org.koitharu.kotatsu.image.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import androidx.annotation.Px
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.UnknownMangaSource
import org.koitharu.kotatsu.core.ui.widgets.StackLayout
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.databinding.ViewCoverStackBinding
import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource

class CoverStackView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : StackLayout(context, attrs, defStyleAttr) {

	private val binding = ViewCoverStackBinding.inflate(LayoutInflater.from(context), this)
	private val coverViews = arrayOf(
		binding.imageViewCover1,
		binding.imageViewCover2,
		binding.imageViewCover3,
	)
	private var hideEmptyView: Boolean = false

	init {
		context.withStyledAttributes(attrs, R.styleable.CoverStackView, defStyleAttr) {
			hideEmptyView = getBoolean(R.styleable.CoverStackView_hideEmptyViews, hideEmptyView)
			children.forEach { it.isGone = hideEmptyView }
			val coverSize = getDimension(R.styleable.CoverStackView_coverSize, 0f)
			if (coverSize > 0f) {
				setCoverSize(coverSize)
			}
		}
		val backgroundColor = context.getThemeColor(android.R.attr.colorBackground)
		ImageViewCompat.setImageTintList(
			binding.imageViewCover3,
			ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153)),
		)
		ImageViewCompat.setImageTintList(
			binding.imageViewCover2,
			ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76)),
		)
		binding.imageViewCover2.backgroundTintList = ColorStateList.valueOf(
			ColorUtils.setAlphaComponent(backgroundColor, 76),
		)
		binding.imageViewCover3.backgroundTintList = ColorStateList.valueOf(
			ColorUtils.setAlphaComponent(backgroundColor, 153),
		)
		coverViews.forEachIndexed { index, view ->
			view.crossfadeDurationFactor = index + 1f
		}
	}

	fun setCoversAsync(covers: List<Cover>) {
		coverViews.forEachIndexed { index, view ->
			view.setImageAsync(covers.getOrNull(index))
		}
	}

	@JvmName("setMangaCoversAsync")
	fun setCoversAsync(manga: List<Manga>) {
		coverViews.forEachIndexed { index, view ->
			val m = manga.getOrNull(index)
			view.setCoverOrHide(m?.coverUrl, m, m?.source)
		}
	}

	fun setCoverSize(@Px coverSize: Float) {
		val coverWidth = (coverSize * 13f).toInt()
		val coverHeight = (coverSize * 18f).toInt()
		children.forEach {
			it.updateLayoutParams {
				width = coverWidth
				height = coverHeight
			}
		}
	}

	private fun CoverImageView.setCoverOrHide(url: String?, manga: Manga?, source: MangaSource?) {
		if (url.isNullOrEmpty() && hideEmptyView) {
			disposeImage()
			isVisible = false
		} else {
			isVisible = true
			if (manga != null) {
				setImageAsync(url, manga)
			} else {
				setImageAsync(url, source ?: UnknownMangaSource)
			}
		}
	}
}
