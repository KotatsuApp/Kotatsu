package org.koitharu.kotatsu.ui.details

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_chapters.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.reader.ReaderActivity

class ChaptersFragment : BaseFragment(R.layout.fragment_chapters), MangaDetailsView,
	OnRecyclerItemClickListener<MangaChapter> {

	@Suppress("unused")
	private val presenter by moxyPresenter { (activity as MangaDetailsActivity).presenter }

	private var manga: Manga? = null

	private lateinit var adapter: ChaptersAdapter

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = ChaptersAdapter(this)
		recyclerView_chapters.addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))
		recyclerView_chapters.adapter = adapter
	}

	override fun onMangaUpdated(manga: Manga) {
		this.manga = manga
		adapter.replaceData(manga.chapters.orEmpty())
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onError(e: Exception) {

	}

	override fun onItemClick(item: MangaChapter, position: Int, view: View) {
		startActivity(ReaderActivity.newIntent(
			context ?: return,
			manga ?: return,
			item.id
		))
	}
}