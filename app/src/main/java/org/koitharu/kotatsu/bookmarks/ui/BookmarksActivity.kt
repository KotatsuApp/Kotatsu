package org.koitharu.kotatsu.bookmarks.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityContainerBinding

@AndroidEntryPoint
class BookmarksActivity : BaseActivity<ActivityContainerBinding>() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				val fragment = BookmarksFragment.newInstance()
				replace(R.id.container, fragment)
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		with(binding.toolbar) {
			updatePadding(
				left = insets.left,
				right = insets.right,
			)
		}
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, BookmarksActivity::class.java)
	}
}
