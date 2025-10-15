package org.koitharu.kotatsu.image.ui

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.size.Dimension
import coil3.size.Size
import coil3.size.ViewSizeResolver
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.FileNotFoundException
import org.jsoup.HttpStatusException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.core.image.CoilImageView
import org.koitharu.kotatsu.core.ui.image.AnimatedPlaceholderDrawable
import org.koitharu.kotatsu.core.ui.image.TextDrawable
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.util.ext.bookmarkExtra
import org.koitharu.kotatsu.core.util.ext.decodeRegion
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.isNetworkError
import org.koitharu.kotatsu.core.util.ext.mangaExtra
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.parsers.exception.ContentUnavailableException
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.exception.TooManyRequestExceptions
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import kotlin.coroutines.resume
import androidx.appcompat.R as appcompatR
import com.google.android.material.R as materialR

class CoverImageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.coverImageViewStyle,
) : CoilImageView(context, attrs, defStyleAttr) {

	private var aspectRationHeight: Int = 0
	private var aspectRationWidth: Int = 0
	var trimImage: Boolean = false

	private val hasAspectRatio: Boolean
		get() = aspectRationHeight > 0 && aspectRationWidth > 0

	init {
		context.withStyledAttributes(attrs, R.styleable.CoverImageView, defStyleAttr) {
			aspectRationHeight = getInt(R.styleable.CoverImageView_aspectRationHeight, aspectRationHeight)
			aspectRationWidth = getInt(R.styleable.CoverImageView_aspectRationWidth, aspectRationWidth)
			trimImage = getBoolean(R.styleable.CoverImageView_trimImage, trimImage)
		}
		if (placeholderDrawable == null) {
			placeholderDrawable = AnimatedPlaceholderDrawable(context)
		}
		if (errorDrawable == null) {
			errorDrawable = ColorUtils.blendARGB(
				context.getThemeColor(materialR.attr.colorErrorContainer),
				context.getThemeColor(appcompatR.attr.colorBackgroundFloating),
				0.25f,
			).toDrawable()
		}
		if (fallbackDrawable == null) {
			fallbackDrawable = context.getThemeColor(materialR.attr.colorSurfaceContainer).toDrawable()
		}
		addImageRequestListener(ErrorForegroundListener())
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		if (!hasAspectRatio) {
			return
		}
		val isExactWidth = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
		val isExactHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
		when {
			isExactHeight && isExactWidth -> Unit
			isExactHeight -> setMeasuredDimension(
				/* measuredWidth = */ measuredHeight * aspectRationWidth / aspectRationHeight,
				/* measuredHeight = */ measuredHeight,
			)

			isExactWidth -> setMeasuredDimension(
				/* measuredWidth = */ measuredWidth,
				/* measuredHeight = */ measuredWidth * aspectRationHeight / aspectRationWidth,
			)
		}
	}

	fun setImageAsync(page: ReaderPage) = enqueueRequest(
		newRequestBuilder()
			.data(page.toMangaPage())
			.mangaSourceExtra(page.source)
			.build(),
	)

	fun setImageAsync(page: MangaPage) = enqueueRequest(
		newRequestBuilder()
			.data(page)
			.mangaSourceExtra(page.source)
			.build(),
	)

	fun setImageAsync(cover: Cover?) = enqueueRequest(
		newRequestBuilder()
			.data(cover?.url)
			.mangaSourceExtra(cover?.mangaSource)
			.build(),
	)

	fun setImageAsync(
		coverUrl: String?,
		manga: Manga?,
	) = enqueueRequest(
		newRequestBuilder()
			.data(coverUrl)
			.mangaExtra(manga)
			.build(),
	)

	fun setImageAsync(
		coverUrl: String?,
		source: MangaSource,
	) = enqueueRequest(
		newRequestBuilder()
			.data(coverUrl)
			.mangaSourceExtra(source)
			.build(),
	)

	fun setImageAsync(
		bookmark: Bookmark
	) = enqueueRequest(
		newRequestBuilder()
			.data(bookmark.toMangaPage())
			.decodeRegion(bookmark.scroll)
			.bookmarkExtra(bookmark)
			.build(),
	)

	override fun newRequestBuilder() = super.newRequestBuilder().apply {
		if (trimImage) {
			transformations(listOf(TrimTransformation()))
		}
		if (hasAspectRatio) {
			size(CoverSizeResolver(this@CoverImageView))
		}
	}

	private inner class ErrorForegroundListener : ImageRequest.Listener {

		override fun onSuccess(request: ImageRequest, result: SuccessResult) {
			super.onSuccess(request, result)
			foreground = null
		}

		override fun onCancel(request: ImageRequest) {
			super.onCancel(request)
			foreground = null
		}

		override fun onStart(request: ImageRequest) {
			super.onStart(request)
			foreground = null
		}

		override fun onError(request: ImageRequest, result: ErrorResult) {
			super.onError(request, result)
			foreground = if (result.throwable.isNetworkError() && !networkState.isOnline()) {
				ContextCompat.getDrawable(context, R.drawable.ic_offline)?.let {
					LayerDrawable(arrayOf(it)).apply {
						setLayerGravity(0, Gravity.CENTER)
						setTint(ContextCompat.getColor(context, R.color.dim_lite))
					}
				}
			} else {
				result.throwable.getShortMessage()?.let { text ->
					TextDrawable.create(context, text, materialR.attr.textAppearanceTitleSmall)
				}
			}
		}

		private fun Throwable.getShortMessage(): String? = when (this) {
			is HttpException -> response.code.toString()
			is HttpStatusException -> statusCode.toString()
			is ContentUnavailableException,
			is FileNotFoundException -> "404"

			is TooManyRequestExceptions -> "429"
			is ParseException -> "</>"
			is UnsupportedSourceException -> "X"
			is CloudFlareProtectedException -> "?"
			else -> cause?.getShortMessage()
		}
	}

	private class CoverSizeResolver(
		override val view: CoverImageView,
	) : ViewSizeResolver<CoverImageView> {

		override suspend fun size(): Size {
			// Fast path: the view is already measured.
			getSize()?.let { return it }

			// Slow path: wait for the view to be measured.
			return suspendCancellableCoroutine { continuation ->
				val viewTreeObserver = view.viewTreeObserver

				val preDrawListener = object : OnPreDrawListener {
					private var isResumed = false

					override fun onPreDraw(): Boolean {
						val size = getSize()
						if (size != null) {
							viewTreeObserver.removePreDrawListenerSafe(this)

							if (!isResumed) {
								isResumed = true
								continuation.resume(size)
							}
						}
						return true
					}
				}

				viewTreeObserver.addOnPreDrawListener(preDrawListener)

				continuation.invokeOnCancellation {
					viewTreeObserver.removePreDrawListenerSafe(preDrawListener)
				}
			}
		}

		private fun getSize(): Size? {
			var width = getWidth()
			var height = getHeight()
			when {
				width == null && height == null -> {
					return null
				}

				height == null -> {
					height = Dimension(width!!.px * view.aspectRationHeight / view.aspectRationWidth)
				}

				width == null -> {
					width = Dimension(height.px * view.aspectRationWidth / view.aspectRationHeight)
				}
			}
			return Size(width, height)
		}

		private fun getWidth() = getDimension(
			paramSize = view.layoutParams?.width ?: -1,
			viewSize = view.width,
			paddingSize = if (subtractPadding) view.paddingLeft + view.paddingRight else 0,
		)

		private fun getHeight() = getDimension(
			paramSize = view.layoutParams?.height ?: -1,
			viewSize = view.height,
			paddingSize = if (subtractPadding) view.paddingTop + view.paddingBottom else 0,
		)

		private fun getDimension(paramSize: Int, viewSize: Int, paddingSize: Int): Dimension.Pixels? {
			if (paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
				return null
			}
			val insetParamSize = paramSize - paddingSize
			if (insetParamSize > 0) {
				return Dimension(insetParamSize)
			}
			val insetViewSize = viewSize - paddingSize
			if (insetViewSize > 0) {
				return Dimension(insetViewSize)
			}
			return null
		}

		private fun ViewTreeObserver.removePreDrawListenerSafe(victim: OnPreDrawListener) {
			if (isAlive) {
				removeOnPreDrawListener(victim)
			} else {
				view.viewTreeObserver.removeOnPreDrawListener(victim)
			}
		}
	}
}
