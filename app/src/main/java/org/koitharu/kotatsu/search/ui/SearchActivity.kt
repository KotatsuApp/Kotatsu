package org.koitharu.kotatsu.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.databinding.ActivitySearchBinding
import org.koitharu.kotatsu.utils.ext.showKeyboard

class SearchActivity : BaseActivity<ActivitySearchBinding>(), SearchView.OnQueryTextListener {

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
			suggestionsAdapter = MangaSuggestionsProvider.getSuggestionAdapter(this@SearchActivity)
			setOnSuggestionListener(SearchHelper.SuggestionListener(this))
			setOnQueryTextListener(this@SearchActivity)

			if (query.isNullOrBlank()) {
				requestFocus()
				showKeyboard()
			} else {
				setQuery(query, true)
			}
		}
	}

	override fun onDestroy() {
		binding.searchView.suggestionsAdapter.changeCursor(null) //close cursor
		super.onDestroy()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.toolbar.updatePadding(
			top = insets.top,
			left = insets.left,
			right = insets.right
		)
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		return if (!query.isNullOrBlank()) {
			title = query
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.container, SearchFragment.newInstance(source, query))
				.commit()
			binding.searchView.clearFocus()
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