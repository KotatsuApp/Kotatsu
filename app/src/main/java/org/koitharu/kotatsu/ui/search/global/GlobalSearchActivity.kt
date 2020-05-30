package org.koitharu.kotatsu.ui.search.global

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseActivity

class GlobalSearchActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_search)
		val query = intent.getStringExtra(EXTRA_QUERY)

		if (query == null) {
			finish()
			return
		}

		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		title = query
		supportActionBar?.subtitle = getString(R.string.search_results)
		supportFragmentManager
			.beginTransaction()
			.replace(R.id.container, GlobalSearchFragment.newInstance(query))
			.commit()
	}

	companion object {

		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, query: String) =
			Intent(context, GlobalSearchActivity::class.java)
				.putExtra(EXTRA_QUERY, query)
	}
}