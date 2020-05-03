package org.koitharu.kotatsu.ui.widget.recent

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import coil.Coil
import coil.request.GetRequestBuilder
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.requireBitmap
import java.io.IOException

class RecentListFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

	private val dataSet = ArrayList<Manga>()

	override fun onCreate() {
	}

	override fun getLoadingView() = null

	override fun getItemId(position: Int) = dataSet[position].id

	override fun onDataSetChanged() {
		dataSet.clear()
		val data = runBlocking { HistoryRepository().getList(0, 10) }
		dataSet.addAll(data)
	}

	override fun hasStableIds() = true

	override fun getViewAt(position: Int): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.item_recent)
		val item = dataSet[position]
		try {
			val cover = runBlocking {
				Coil.execute(GetRequestBuilder(context)
					.data(item.coverUrl)
					.build()).requireBitmap()
			}
			views.setImageViewBitmap(R.id.imageView_cover, cover)
		} catch (e: IOException) {
			views.setImageViewResource(R.id.imageView_cover, R.drawable.ic_placeholder)
		}
		val intent = Intent()
		intent.putExtra(MangaDetailsActivity.EXTRA_MANGA_ID, item.id)
		views.setOnClickFillInIntent(R.id.imageView_cover, intent)
		return views
	}

	override fun getCount() = dataSet.size

	override fun getViewTypeCount() = 1

	override fun onDestroy() {
	}
}