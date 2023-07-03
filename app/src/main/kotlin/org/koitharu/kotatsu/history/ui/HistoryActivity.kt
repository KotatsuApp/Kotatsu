package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityContainerBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner

@AndroidEntryPoint
class HistoryActivity :
	BaseActivity<ActivityContainerBinding>(),
	AppBarOwner {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				setReorderingAllowed(true)
				val fragment = HistoryListFragment()
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

		fun newIntent(context: Context) = Intent(context, HistoryActivity::class.java)
	}
}
