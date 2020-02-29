package org.koitharu.kotatsu.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.BaseActivity

class SearchActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_search)
		val source = intent.getParcelableExtra<MangaSource>(EXTRA_SOURCE)
		val query = intent.getStringExtra(EXTRA_QUERY)

		if (source == null || query == null) {
			finish()
			return
		}

		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		title = query
		supportActionBar?.setSubtitle(R.string.search_results)
		supportFragmentManager
			.beginTransaction()
			.replace(R.id.container, SearchFragment.newInstance(source, query))
			.commit()
	}

	companion object {

		private const val EXTRA_SOURCE = "source"
		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, source: MangaSource, query: String) =
			Intent(context, SearchActivity::class.java)
				.putExtra(EXTRA_SOURCE, source as Parcelable)
				.putExtra(EXTRA_QUERY, query)
	}
}