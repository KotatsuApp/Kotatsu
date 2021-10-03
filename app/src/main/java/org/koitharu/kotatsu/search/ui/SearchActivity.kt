package org.koitharu.kotatsu.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.databinding.ActivitySearchBinding
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionViewModel
import org.koitharu.kotatsu.utils.ext.showKeyboard

class SearchActivity : BaseActivity<ActivitySearchBinding>(), SearchView.OnQueryTextListener {

	private val searchSuggestionViewModel by viewModel<SearchSuggestionViewModel>(
		mode = LazyThreadSafetyMode.NONE
	)
	private lateinit var source: MangaSource

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchBinding.inflate(layoutInflater))
		source = intent.getParcelableExtra(EXTRA_SOURCE) ?: run {
			finishAfterTransition()
			return
		}
		val query = intent.getStringExtra(EXTRA_QUERY)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		with(binding.searchView) {
			queryHint = getString(R.string.search_on_s, source.title)
			setOnQueryTextListener(this@SearchActivity)

			if (query.isNullOrBlank()) {
				requestFocus()
				showKeyboard()
			} else {
				setQuery(query, true)
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.toolbar.updatePadding(
			top = insets.top,
			left = insets.left,
			right = insets.right
		)
		binding.container.updatePadding(
			bottom = insets.bottom
		)
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		val q = query?.trim()
		if (q.isNullOrEmpty()) {
			return false
		}
		title = query
		supportFragmentManager.commit {
			replace(R.id.container, SearchFragment.newInstance(source, q))
		}
		binding.searchView.clearFocus()
		searchSuggestionViewModel.saveQuery(q)
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean = false

	companion object {

		private const val EXTRA_SOURCE = "source"
		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, source: MangaSource, query: String?) =
			Intent(context, SearchActivity::class.java)
				.putExtra(EXTRA_SOURCE, source as Parcelable)
				.putExtra(EXTRA_QUERY, query)
	}
}