package org.koitharu.kotatsu.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.widget.SearchView
import kotlinx.android.synthetic.main.activity_search.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.base.BaseActivity
import org.koitharu.kotatsu.utils.ext.showKeyboard

class SearchActivity : BaseActivity(), SearchView.OnQueryTextListener {

	private lateinit var source: MangaSource

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_search)
		source = intent.getParcelableExtra(EXTRA_SOURCE) ?: run {
			finishAfterTransition()
			return
		}
		val query = intent.getStringExtra(EXTRA_QUERY)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		searchView.queryHint = getString(R.string.search_on_s, source.title)
		searchView.suggestionsAdapter = MangaSuggestionsProvider.getSuggestionAdapter(this)
		searchView.setOnSuggestionListener(SearchHelper.SuggestionListener(searchView))
		searchView.setOnQueryTextListener(this)

		if (query.isNullOrBlank()) {
			searchView.requestFocus()
			searchView.showKeyboard()
		} else {
			searchView.setQuery(query, true)
		}
	}

	override fun onDestroy() {
		searchView.suggestionsAdapter?.changeCursor(null) //close cursor
		super.onDestroy()
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		return if (!query.isNullOrBlank()) {
			title = query
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.container, SearchFragment.newInstance(source, query))
				.commit()
			searchView.clearFocus()
			MangaSuggestionsProvider.saveQueryAsync(applicationContext, query)
			true
		} else false
	}

	override fun onQueryTextChange(newText: String?) = false

	companion object {

		private const val EXTRA_SOURCE = "source"
		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, source: MangaSource, query: String?) =
			Intent(context, SearchActivity::class.java)
				.putExtra(EXTRA_SOURCE, source as Parcelable)
				.putExtra(EXTRA_QUERY, query)
	}
}