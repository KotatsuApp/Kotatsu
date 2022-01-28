package org.koitharu.kotatsu.search.ui.global

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivitySearchGlobalBinding

class GlobalSearchActivity : BaseActivity<ActivitySearchGlobalBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchGlobalBinding.inflate(layoutInflater))
		val query = intent.getStringExtra(EXTRA_QUERY)

		if (query == null) {
			finishAfterTransition()
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

	override fun onWindowInsetsChanged(insets: Insets) {
		with(binding.toolbar) {
			updatePadding(
				left = insets.left,
				right = insets.right
			)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
	}

	companion object {

		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, query: String) =
			Intent(context, GlobalSearchActivity::class.java)
				.putExtra(EXTRA_QUERY, query)
	}
}