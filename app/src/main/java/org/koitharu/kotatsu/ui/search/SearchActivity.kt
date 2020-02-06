package org.koitharu.kotatsu.ui.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseActivity

class SearchActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_search)
		val query = if (Intent.ACTION_SEARCH == intent.action) {
			intent.getStringExtra(SearchManager.QUERY)?.trim()
		} else {
			null
		}
		if (query == null) {
			finish()
			return
		}
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		title = query
		supportActionBar?.setSubtitle(R.string.search_results)
		supportFragmentManager
			.beginTransaction()
			.replace(R.id.container, SearchFragment.newInstance(query))
			.commit()
	}
}