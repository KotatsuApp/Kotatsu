package org.koitharu.kotatsu.favourites.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityContainerBinding
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment.Companion.NO_ID

@AndroidEntryPoint
class FavouritesActivity : BaseActivity<ActivityContainerBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val categoryTitle = intent.getStringExtra(EXTRA_TITLE)
		if (categoryTitle != null) {
			title = categoryTitle
		}
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				setReorderingAllowed(true)
				val fragment = FavouritesListFragment.newInstance(intent.getLongExtra(EXTRA_CATEGORY_ID, NO_ID))
				replace(R.id.container, fragment)
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	companion object {

		private const val EXTRA_CATEGORY_ID = "cat_id"
		private const val EXTRA_TITLE = "title"

		fun newIntent(context: Context) = Intent(context, FavouritesActivity::class.java)

		fun newIntent(context: Context, category: FavouriteCategory) = Intent(context, FavouritesActivity::class.java)
			.putExtra(EXTRA_CATEGORY_ID, category.id)
			.putExtra(EXTRA_TITLE, category.title)
	}
}
