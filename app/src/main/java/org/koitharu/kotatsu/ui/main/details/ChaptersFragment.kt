package org.koitharu.kotatsu.ui.main.details

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_chapters.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.common.BaseFragment

class ChaptersFragment : BaseFragment(R.layout.fragment_chapters), MangaDetailsView {

	private val presenter by moxyPresenter { (activity as MangaDetailsActivity).presenter }

	private lateinit var adapter: ChaptersAdapter

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = ChaptersAdapter {

		}
		recyclerView_chapters.addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))
		recyclerView_chapters.adapter = adapter
	}

	override fun onMangaUpdated(manga: Manga) {
		adapter.replaceData(manga.chapters.orEmpty())
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onError(e: Exception) {

	}
}