package org.koitharu.kotatsu.ui.reader.thumbnails

import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import kotlin.coroutines.CoroutineContext

class PagesThumbnailsAdapter(onItemClickListener: OnRecyclerItemClickListener<MangaPage>?) :
    BaseRecyclerAdapter<MangaPage, Unit>(onItemClickListener), CoroutineScope, DisposableHandle {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun dispose() {
        job.cancel()
    }

    override fun getExtra(item: MangaPage, position: Int) = Unit

    override fun onCreateViewHolder(parent: ViewGroup) = PageThumbnailHolder(parent, this)

    override fun onGetItemId(item: MangaPage) = item.id
}