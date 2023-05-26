package org.koitharu.kotatsu.widget.recent

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.executeBlocking
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.util.ext.getDrawableOrThrow
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.replaceWith

class RecentListFactory(
	private val context: Context,
	private val historyRepository: HistoryRepository,
	private val coil: ImageLoader,
) : RemoteViewsService.RemoteViewsFactory {

	private val dataSet = ArrayList<Manga>()
	private val transformation = RoundedCornersTransformation(
		context.resources.getDimension(R.dimen.appwidget_corner_radius_inner),
	)
	private val coverSize = Size(
		context.resources.getDimensionPixelSize(R.dimen.widget_cover_width),
		context.resources.getDimensionPixelSize(R.dimen.widget_cover_height),
	)

	override fun onCreate() = Unit

	override fun getLoadingView() = null

	override fun getItemId(position: Int) = dataSet[position].id

	override fun onDataSetChanged() {
		val data = runBlocking { historyRepository.getList(0, 10) }
		dataSet.replaceWith(data)
	}

	override fun hasStableIds() = true

	override fun getViewAt(position: Int): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.item_recent)
		val item = dataSet[position]
		runCatching {
			coil.executeBlocking(
				ImageRequest.Builder(context)
					.data(item.coverUrl)
					.size(coverSize)
					.tag(item.source)
					.transformations(transformation)
					.build(),
			).getDrawableOrThrow().toBitmap()
		}.onSuccess { cover ->
			views.setImageViewBitmap(R.id.imageView_cover, cover)
		}.onFailure {
			views.setImageViewResource(R.id.imageView_cover, R.drawable.ic_placeholder)
		}
		val intent = Intent()
		intent.putExtra(MangaIntent.KEY_ID, item.id)
		views.setOnClickFillInIntent(R.id.imageView_cover, intent)
		return views
	}

	override fun getCount() = dataSet.size

	override fun getViewTypeCount() = 1

	override fun onDestroy() = Unit
}
