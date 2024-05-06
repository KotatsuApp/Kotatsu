package org.koitharu.kotatsu.bookmarks.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityContainerBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.main.ui.owners.SnackbarOwner

@AndroidEntryPoint
class AllBookmarksActivity :
	BaseActivity<ActivityContainerBinding>(),
	AppBarOwner,
	SnackbarOwner {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override val snackbarHost: CoordinatorLayout
		get() = viewBinding.root

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				setReorderingAllowed(true)
				replace(R.id.container, AllBookmarksFragment::class.java, null)
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

		fun newIntent(context: Context) = Intent(context, AllBookmarksActivity::class.java)
	}
}
