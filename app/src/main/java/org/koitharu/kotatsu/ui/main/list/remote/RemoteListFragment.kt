package org.koitharu.kotatsu.ui.main.list.remote

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.main.list.MangaListFragment
import org.koitharu.kotatsu.utils.ext.withArgs

class RemoteListFragment : MangaListFragment<Unit>() {

	private val presenter by moxyPresenter(factory = ::RemoteListPresenter)

	private val source by arg<MangaSource>(ARG_SOURCE)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		textView_holder.setText(R.string.nothing_found)
	}

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(source, offset)
	}

	override fun getTitle(): CharSequence? {
		return source.title
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putParcelable(ARG_SOURCE, provider)
		}
	}
}