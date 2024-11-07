package org.koitharu.kotatsu.widget.shelf

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.executeBlocking
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size
import coil3.transform.RoundedCornersTransformation
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.AppWidgetConfig
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.util.ext.getDrawableOrThrow
import org.koitharu.kotatsu.core.util.ext.mangaExtra
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.replaceWith

class ShelfListFactory(
	private val context: Context,
	private val favouritesRepository: FavouritesRepository,
	private val coilLazy: Lazy<ImageLoader>,
	private val settings: AppSettings,
	widgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {

	private val dataSet = ArrayList<Manga>()
	private val config = AppWidgetConfig(context, ShelfWidgetProvider::class.java, widgetId)
	private val transformation = RoundedCornersTransformation(
		context.resources.getDimension(R.dimen.appwidget_corner_radius_inner),
	)
	private val coverSize = Size(
		context.resources.getDimensionPixelSize(R.dimen.widget_cover_width),
		context.resources.getDimensionPixelSize(R.dimen.widget_cover_height),
	)

	override fun onCreate() = Unit

	override fun getLoadingView() = null

	override fun getItemId(position: Int) = dataSet.getOrNull(position)?.id ?: 0L

	override fun onDataSetChanged() {
		val data = if (settings.appPassword.isNullOrEmpty()) {
			runBlocking {
				val category = config.categoryId
				if (category == 0L) {
					favouritesRepository.getAllManga()
				} else {
					favouritesRepository.getManga(category)
				}
			}
		} else {
			emptyList()
		}
		dataSet.replaceWith(data)
	}

	override fun hasStableIds() = true

	override fun getViewAt(position: Int): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.item_shelf)
		val item = dataSet.getOrNull(position) ?: return views
		views.setTextViewText(R.id.textView_title, item.title)
		runCatching {
			coilLazy.get().executeBlocking(
				ImageRequest.Builder(context)
					.data(item.coverUrl)
					.size(coverSize)
					.mangaExtra(item)
					.transformations(transformation, TrimTransformation())
					.build(),
			).getDrawableOrThrow().toBitmap()
		}.onSuccess { cover ->
			views.setImageViewBitmap(R.id.imageView_cover, cover)
		}.onFailure {
			views.setImageViewResource(R.id.imageView_cover, R.drawable.ic_placeholder)
		}
		val intent = Intent()
		intent.putExtra(MangaIntent.KEY_ID, item.id)
		views.setOnClickFillInIntent(R.id.rootLayout, intent)
		return views
	}

	override fun getCount() = dataSet.size

	override fun getViewTypeCount() = 1

	override fun onDestroy() = Unit
}
