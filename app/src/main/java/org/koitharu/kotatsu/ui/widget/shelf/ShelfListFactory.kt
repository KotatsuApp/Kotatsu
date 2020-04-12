package org.koitharu.kotatsu.ui.widget.shelf

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.api.get
import kotlinx.coroutines.runBlocking
import okio.IOException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity

class ShelfListFactory(context: Context, private val intent: Intent) : RemoteViewsService.RemoteViewsFactory {

	private val packageName = context.packageName

	private val dataSet = ArrayList<Manga>()

	override fun onCreate() {
	}

	override fun getLoadingView() = null

	override fun getItemId(position: Int) = dataSet[position].id

	override fun onDataSetChanged() {
		dataSet.clear()
		val data = runBlocking { FavouritesRepository().getAllManga(0) }
		dataSet.addAll(data)
	}

	override fun hasStableIds() = true

	override fun getViewAt(position: Int): RemoteViews {
		val views = RemoteViews(packageName, R.layout.item_shelf)
		val item = dataSet[position]
		views.setTextViewText(R.id.textView_title, item.title)
		try {
			val cover = runBlocking {
				Coil.loader().get(item.coverUrl).toBitmap()
			}
			views.setImageViewBitmap(R.id.imageView_cover, cover)
		} catch (e: IOException) {
			views.setImageViewResource(R.id.imageView_cover, R.drawable.ic_placeholder)
		}
		val intent = Intent()
		intent.putExtra(MangaDetailsActivity.EXTRA_MANGA_ID, item.id)
		views.setOnClickFillInIntent(R.id.rootLayout, intent)
		return views
	}

	override fun getCount() = dataSet.size

	override fun getViewTypeCount() = 1

	override fun onDestroy() {
	}
}