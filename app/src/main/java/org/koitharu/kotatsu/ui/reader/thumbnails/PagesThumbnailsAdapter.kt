package org.koitharu.kotatsu.ui.reader.thumbnails

import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.base.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.base.list.OnRecyclerItemClickListener
import kotlin.coroutines.CoroutineContext

class PagesThumbnailsAdapter(onItemClickListener: OnRecyclerItemClickListener<MangaPage>?) :
	BaseRecyclerAdapter<MangaPage, PagesCache>(onItemClickListener), CoroutineScope,
	DisposableHandle {

	private val job = SupervisorJob()
	private val cache by inject<PagesCache>()

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main.immediate + job

	override fun dispose() {
		job.cancel()
	}

	override fun getExtra(item: MangaPage, position: Int) = cache

	override fun onCreateViewHolder(parent: ViewGroup) = PageThumbnailHolder(parent, this)

	override fun onGetItemId(item: MangaPage) = item.id
}