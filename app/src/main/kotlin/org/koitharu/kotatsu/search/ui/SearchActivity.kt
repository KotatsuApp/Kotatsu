package org.koitharu.kotatsu.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.showKeyboard
import org.koitharu.kotatsu.databinding.ActivitySearchBinding
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionViewModel

@AndroidEntryPoint
class SearchActivity : BaseActivity<ActivitySearchBinding>(), SearchView.OnQueryTextListener {

	private val searchSuggestionViewModel by viewModels<SearchSuggestionViewModel>()
	private lateinit var source: MangaSource

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchBinding.inflate(layoutInflater))
		source = intent.getSerializableExtraCompat(EXTRA_SOURCE) ?: run {
			finishAfterTransition()
			return
		}
		val query = intent.getStringExtra(EXTRA_QUERY)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		searchSuggestionViewModel.isIncognitoModeEnabled.observe(this, this::onIncognitoModeChanged)
		with(viewBinding.searchView) {
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
		viewBinding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.container.updatePadding(
			bottom = insets.bottom,
		)
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		val q = query?.trim()
		if (q.isNullOrEmpty()) {
			return false
		}
		title = query
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, SearchFragment.newInstance(source, q))
		}
		viewBinding.searchView.clearFocus()
		searchSuggestionViewModel.saveQuery(q)
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean = false

	private fun onIncognitoModeChanged(isIncognito: Boolean) {
		var options = viewBinding.searchView.imeOptions
		options = if (isIncognito) {
			options or EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING
		} else {
			options and EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
		}
		viewBinding.searchView.imeOptions = options
	}

	companion object {

		private const val EXTRA_SOURCE = "source"
		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, source: MangaSource, query: String?) =
			Intent(context, SearchActivity::class.java)
				.putExtra(EXTRA_SOURCE, source)
				.putExtra(EXTRA_QUERY, query)
	}
}
